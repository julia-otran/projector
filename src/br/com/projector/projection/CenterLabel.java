/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection;

import br.com.projector.projection.text.TextWrapper;
import br.com.projector.projection.text.CommaTextWrapper;
import br.com.projector.projection.models.StringWithPosition;
import br.com.projector.projection.models.StringWithWidth;
import br.com.projector.projection.text.WrapperFactory;
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
    private static final int DEFAULT_PADDING = 40;
    private static final int DEFAULT_FONT_SIZE = 110;
    
    // Customizations
    private TextWrapper wrapper;
    private String text;
    private int padding = DEFAULT_PADDING;
    
    // Internal control
    private final List<StringWithPosition> drawLines = new ArrayList<>();
    private int fontHeight;
    
    private WrapperFactory wrapperFactory;
    
    public void initialize() {
        wrapperFactory = new WrapperFactory(getFreeWidth(), getFreeHeight());
        Font font = getFont();
        
        if (font.getSize() < DEFAULT_FONT_SIZE) {
            font = new java.awt.Font(font.getName(), 0, DEFAULT_FONT_SIZE);
            setFont(font);
        }
        
        wrapper = wrapperFactory.getTextWrapper(getFontMetrics(font), true);
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
        
        drawLines.forEach((pt) -> {
            g2.drawString(pt.getText(), pt.getX(), pt.getY());
        });
    }

    @Override
    public void setFont(Font font) {
        FontMetrics fm = getFontMetrics(font);
        fontHeight = fm.getHeight();
        wrapper = wrapperFactory.getTextWrapper(getFontMetrics(getFont()), true);
        super.setFont(font);
    }
    
    @Override
    public void revalidate() {
        rebuildText();
        super.revalidate();
    }
    
    public TextWrapper getWrapper() {
        return wrapper;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
        revalidate();
    }
    
    public String getText() {
        return text;
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
        List<StringWithWidth> wraps = wrapper.wrap(text);
        
        List<StringWithPosition> strings = wraps.stream()
            .map(sw -> { 
                int x = ((freeW - sw.getWidth()) / 2) + padding;
                return new StringWithPosition(sw.getString(), x, 0);
            })
            .collect(Collectors.toList());
        
        int translateY = (getHeight() + fontHeight - (fontHeight * wraps.size())) / 2;
        translateY += padding;
        
        for (StringWithPosition s : strings) {
            s.setY(translateY);
            translateY += fontHeight;
        }
        
        drawLines.addAll(strings);
    }
    
    private int getFreeWidth() {
        return getWidth() - 2 * padding;
    }
    
    private int getFreeHeight() {
        return getHeight() - 2 * padding;
    }
}
