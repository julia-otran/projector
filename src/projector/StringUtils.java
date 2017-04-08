/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

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
public class StringUtils {
    public static class StringWithWidth {
        String string;
        int width;
        
        StringWithWidth(String string, int width) {
            this.string = string;
            this.width = width;
        }
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
   * @param fm
   *          needed for string width calculations
   * @param maxWidth
   *          the max line width, in points
   * @return a non-empty list of strings
   */
  public static List<StringWithWidth> wrap(String str, FontMetrics fm, int maxWidth) {
    List<String> lines = splitIntoLines(str);
    if (lines.isEmpty())
      return Collections.emptyList();

    ArrayList<String> strings = new ArrayList<>();
    for (Iterator<String> iter = lines.iterator(); iter.hasNext();)
      wrapLineInto(iter.next(), strings, fm, maxWidth);
    
    return strings.stream()
            .map(s -> new StringWithWidth(s, fm.stringWidth(s)))
            .collect(Collectors.toList());
  }

  /**
   * Given a line of text and font metrics information, wrap the line and add
   * the new line(s) to <var>list</var>.
   * 
   * @param line
   *          a line of text
   * @param list
   *          an output list of strings
   * @param fm
   *          font metrics
   * @param maxWidth
   *          maximum width of the line(s)
   */
  public static void wrapLineInto(String line, List<String> list, FontMetrics fm, int maxWidth) {
    int len = line.length();
    int width;
    while (len > 0 && (width = fm.stringWidth(line)) > maxWidth) {
      // Guess where to split the line. Look for the next space before
      // or after the guess.
      int guess = len * maxWidth / width;
      String before = line.substring(0, guess).trim();

      width = fm.stringWidth(before);
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
          if (fm.stringWidth(before) > maxWidth)
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
    if (len > 0)
      list.add(line);
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
  public static int findBreakBefore(String line, int start) {
    for (int i = start; i >= 0; --i) {
      char c = line.charAt(i);
      if (Character.isWhitespace(c) || c == '-')
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
  public static int findBreakAfter(String line, int start) {
    int len = line.length();
    for (int i = start; i < len; ++i) {
      char c = line.charAt(i);
      if (Character.isWhitespace(c) || c == '-')
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
  public static List<String> splitIntoLines(String str) {
    final List<String> strings = new ArrayList<>();

    Arrays.asList(str.replace("\r\n", "\n").replace("\r", "\n").split("\n"))
            .stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(line -> {
                int countCommas = countCommas(line);
                if (countCommas > 0) {
                    StringBuilder builder = new StringBuilder();
                    
                    int halfCommas = countCommas / 2;
                    int current = 0;
                    int l = line.length();
                    for (int i = 0; i<l; i++) {
                        char c = line.charAt(i);
                        builder.append(c);
                        if (c == ',') {
                            current++;
                        }
                        if ((countCommas == 1 && current == 1) ||
                            (halfCommas > 0 && current >= halfCommas)) {
                            current = 0;
                            strings.add(builder.toString());
                            builder = new StringBuilder();
                        }
                    }
                    
                    if (builder.length() > 0) {
                        strings.add(builder.toString());
                    }
                } else {
                    strings.add(line);
                }
            });
    

    List<String> splited = strings.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    
    if (splited.isEmpty()) {
        return Collections.singletonList("");
    }
    
    return splited;
  }
  
  private static int countCommas(String s) {
      int l = s.length();
      int c = 0;
      for (int i=0; i<l; i++) {
          if (s.charAt(i) == ',') {
              c++;
          }
      }
      
      return c;
  }
}
