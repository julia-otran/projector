/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;

/**
 *
 * @author guilherme
 */
public class CenterLabel extends JComponent {
    private static final int PADDING = 20;
    private static final int DEFAULT_FONT_SIZE = 110;

    class PrintingText {
        int x;
        String text;
        
        PrintingText(String s, int x) {
            text = s;
            this.x = x;
        }
    }
    
    private String text;
    private final List<PrintingText> drawLines = new ArrayList<>();
    private int fontHeight;
    private int translateY;
    
    public void checkDefaultFontSize() {
        Font font = getFont();
        
        if (font.getSize() < DEFAULT_FONT_SIZE) {
            setFont(new java.awt.Font(font.getName(), 0, DEFAULT_FONT_SIZE));
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(Color.WHITE);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int pos = translateY;
        for (PrintingText pt : drawLines) {
            g2.drawString(pt.text, pt.x + PADDING, pos + PADDING);
            pos += fontHeight;
        }
    }

    @Override
    public void revalidate() {
        FontMetrics fm = getFontMetrics(getFont());
        fontHeight = fm.getHeight();
        rebuildText();
        super.revalidate();
    }
    
    public void setText(String text) {
        this.text = text;
        rebuildText();
        repaint();
    }
    
    private void rebuildText() {
        drawLines.clear();
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        FontMetrics fm = getFontMetrics(getFont());
        
        int freeW = getFreeWidth();
       
        List<StringUtils.StringWithWidth> wraps = StringUtils.wrap(text, fm, freeW);
        
        translateY = getHeight() + fontHeight - (fontHeight * wraps.size());
        translateY /= 2;
        
        drawLines.addAll(wraps.stream()
                .map(sw -> new PrintingText(sw.string, (freeW - sw.width) / 2))
                .collect(Collectors.toList()));
        
    }
    
    private int getFreeWidth() {
        return getWidth() - 2 * PADDING;
    }
}
