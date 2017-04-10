/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection.text;

import br.com.projector.projection.models.StringWithWidth;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author guilherme
 */
public class NormalTextWrapper implements TextWrapper {
    private final FontMetrics fontMetrics;
    private final int maxWidth;
    private final int maxHeight;
    
    NormalTextWrapper(FontMetrics fontMetrics, int maxWidth, int maxHeight) {
        this.fontMetrics = fontMetrics;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
    
    @Override
    public List<List<String>> fitGroups(List<String> phrases) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Returns an array of strings, one for each line in the string after it has
     * been wrapped to fit lines of <var>maxWidth</var>. Lines end with any of
     * cr, lf, or cr lf. A line ending at the end of the string will not output a
     * further, empty string.
     * <p>
     * This code assumes <var>str</var> is not <code>null</code>.
     * 
     * @param str
     *          the string to split
     * @return a list of strings
     */
    @Override
    public List<StringWithWidth> wrap(String str) {
        List<String> lines = splitIntoLines(str);
        
        if (lines.isEmpty())
            return Collections.emptyList();

        ArrayList<String> strings = new ArrayList<>();
        
        for (Iterator<String> iter = lines.iterator(); iter.hasNext();)
            wrapLineInto(iter.next(), strings);

        return strings.stream()
                .map(s -> new StringWithWidth(s, fontMetrics.stringWidth(s)))
                .collect(Collectors.toList());
    }
    
    protected boolean isSeparator(char c) {
        return Character.isWhitespace(c) || c == '-';
    }

    /**
     * Given a line of text and font metrics information, wrap the line and add
     * the new line(s) to <var>list</var>.
     * 
     * @param line
     *          a line of text
     * @param list
     *          an output list of strings
     */
    protected void wrapLineInto(String line, List<String> list) {
        int len = line.length();
        int width;
        while (len > 0 && (width = fontMetrics.stringWidth(line)) > maxWidth) {
            // Guess where to split the line. Look for the next space before
            // or after the guess.
            int guess = len * maxWidth / width;
            String before = line.substring(0, guess).trim();

            width = fontMetrics.stringWidth(before);
            int pos;
            if (width > maxWidth) { 
              // Too long
              pos = findBreakBefore(line, guess);
            } else { 
              // Too short or possibly just right
              pos = findBreakAfter(line, guess);
              if (pos != -1) { 
                // Make sure this doesn't make us too long
                before = line.substring(0, pos).trim();
                if (fontMetrics.stringWidth(before) > maxWidth)
                  pos = findBreakBefore(line, guess);
              } else {
                  int breakBefore = findBreakBefore(line, guess);
                  if (breakBefore > 0) {
                      pos = breakBefore;
                  }
              }
            }
            if (pos == -1)
              pos = guess; // Split in the middle of the word

            list.add(line.substring(0, pos).trim());
            line = line.substring(pos).trim();
            len = line.length();
        }
        
        if (len > 0) {
            list.add(line);
        }
    }

    /**
     * Returns the index of the first whitespace character or '-' in <var>line</var>
     * that is at or before <var>start</var>. Returns -1 if no such character is
     * found.
     * 
     * @param line
     *          a string
     * @param start
     *          where to star looking
     */
    protected int findBreakBefore(String line, int start) {
        for (int i = start; i >= 0; --i) {
            char c = line.charAt(i);
            if (isSeparator(c))
                return i;
        }
        return -1;
    }

    /**
     * Returns the index of the first whitespace character or '-' in <var>line</var>
     * that is at or after <var>start</var>. Returns -1 if no such character is
     * found.
     * 
     * @param line
     *          a string
     * @param start
     *          where to star looking
     */
    protected int findBreakAfter(String line, int start) {
        int len = line.length();
        
        for (int i = start; i < len; ++i) {
            char c = line.charAt(i);
            if (isSeparator(c))
                return i;
        }
        
        return -1;
    }
    
    /**
     * Returns an array of strings, one for each line in the string. Lines end
     * with any of cr, lf, or cr lf. A line ending at the end of the string will
     * not output a further, empty string.
     * <p>
     * This code assumes <var>str</var> is not <code>null</code>.
     * 
     * @param str
     *          the string to split
     * @return a non-empty list of strings
     */
    protected List<String> splitIntoLines(String str) {
        return Arrays.asList(str.replace("\r\n", "\n").replace("\r", "\n").split("\n"))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
