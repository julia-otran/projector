/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection;

import br.com.projector.projection.models.StringWithPosition;
import br.com.projector.projection.text.WrappedText;
import br.com.projector.projection.text.WrapperFactory;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author guilherme
 */
public class ProjectionLabel implements Projectable {
    private CanvasDelegate canvasDelegate;
    
    private static final int DEFAULT_PADDING = 40;
    private static final int DEFAULT_FONT_SIZE = 110;
    
    // Customizations
    private TextWrapperFactoryChangeListener factoryChangeListener;

    private WrappedText text;
    private int padding = DEFAULT_PADDING;
    private Font font;
    private FontMetrics fontMetrics;
    
    // Internal control
    private final List<StringWithPosition> drawLines = new ArrayList<>();
    
    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        font = new java.awt.Font(Font.SANS_SERIF, 0, DEFAULT_FONT_SIZE);
    }
    
    public Font getFont() {
        return font;
    }
    
    public void setFont(Font font) {
        this.font = font;
        this.fontMetrics = canvasDelegate.getFontMetrics(font);
        onFactoryChange();
    }
    
    public int getPadding() {
        return padding;
    }
    
    public void setPadding(int padding) {
        this.padding = padding;
        onFactoryChange();
    }
    
    public WrappedText getText() {
        return text;
    }
    
    public void setText(WrappedText text) {
        this.text = text;
        rebuildLayout();
        repaint();
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(Color.WHITE);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawLines.forEach((pt) -> {
            g2.drawString(pt.getText(), pt.getX(), pt.getY());
        });
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    @Override
    public void setCanvasDelegate(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
    }
    
    private void repaint() {
        canvasDelegate.repaint();
    }
    
    @Override
    public void rebuildLayout() {
        drawLines.clear();
        List<String> lines = text.getLines();
        
        if (lines == null || lines.isEmpty()) {
            return;
        }
        
        int fontHeight = fontMetrics.getHeight();
        int freeW = getFreeWidth();
        
        int translateY = (canvasDelegate.getHeight() + fontHeight - (fontHeight * lines.size())) / 2;
        translateY += padding;
        
        for (String line : lines) {
            int width = fontMetrics.stringWidth(line);
            int x = ((freeW - width) / 2) + padding;
            int y = translateY;

            translateY += fontHeight;

           drawLines.add(new StringWithPosition(line, x, y));
        }
    }
    
    private int getFreeWidth() {
        return canvasDelegate.getWidth() - 2 * padding;
    }
    
    private int getFreeHeight() {
        return canvasDelegate.getHeight() - 2 * padding;
    }
    
    public WrapperFactory getWrapperFactory() {
        return new WrapperFactory(getFreeWidth(), getFreeHeight(), fontMetrics);
    }
    
    private void onFactoryChange() {
        if (factoryChangeListener != null) {
            factoryChangeListener.onWrapperFactoryChanged();
        }
    }
    
    public TextWrapperFactoryChangeListener getWrapperChangeListener() {
        return factoryChangeListener;
    }

    public void setWrapperChangeListener(TextWrapperFactoryChangeListener factoryChangeListener) {
        this.factoryChangeListener = factoryChangeListener;
    }
}
