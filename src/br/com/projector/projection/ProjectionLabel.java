/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection;

import br.com.projector.projection.models.StringWithPosition;
import br.com.projector.projection.text.WrappedText;
import br.com.projector.projection.text.WrapperFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

/**
 *
 * @author guilherme
 */
public class ProjectionLabel implements Projectable {

    private final CanvasDelegate canvasDelegate;

    private static final int DEFAULT_PADDING_X = 120;
    private static final int DEFAULT_PADDING_Y = 40;

    private static final int DEFAULT_FONT_SIZE = 110;

    // Label used to get fontMetrics
    private final JLabel fontLabel;

    // Customizations
    private TextWrapperFactoryChangeListener factoryChangeListener;
    private WrappedText text;
    private int paddingX = DEFAULT_PADDING_X;
    private int paddingY = DEFAULT_PADDING_Y;

    // Internal control
    private final List<StringWithPosition> drawLines = new ArrayList<>();

    private final BasicStroke outlineStroke = new BasicStroke(4.0f);
    private final Color overlay = new Color(0, 0, 0, 240);

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        this.fontLabel = new JLabel();
    }

    @Override
    public void init(ProjectionCanvas source) {
        fontLabel.setVisible(false);
        source.add(fontLabel);
        setFont(new java.awt.Font(Font.SANS_SERIF, 0, DEFAULT_FONT_SIZE));
    }

    public Font getFont() {
        return fontLabel.getFont();
    }

    public void setFont(Font font) {
        fontLabel.setFont(font);
        onFactoryChange();
    }

    public int getPaddingX() {
        return paddingX;
    }

    public int getPaddingY() {
        return paddingY;
    }

    public void setPadding(int paddingX, int paddingY) {
        this.paddingX = paddingX;
        this.paddingY = paddingY;
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
        Graphics2D g2 = (Graphics2D) g;

        if (drawLines.stream().allMatch(p -> p.getText().trim().isEmpty())) {
            return;
        }

        g2.setColor(overlay);
        g2.fillRect(0, 0, canvasDelegate.getWidth(), canvasDelegate.getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setStroke(outlineStroke);

        AffineTransform oldTransform = g2.getTransform();

        drawLines.forEach((pt) -> {
            g2.translate(pt.getX(), pt.getY());
            // create a glyph vector from your text
            GlyphVector glyphVector = fontLabel.getFont().createGlyphVector(g2.getFontRenderContext(), pt.getText());

            // get the shape object
            Shape textShape = glyphVector.getOutline();

            g2.setColor(Color.black);
            g2.draw(textShape);

            g2.setColor(Color.white);
            g2.fill(textShape);

            g2.setTransform(oldTransform);
        });
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    private void repaint() {
        canvasDelegate.repaint();
    }

    @Override
    public void rebuildLayout() {
        drawLines.clear();

        if (text == null) {
            return;
        }

        List<String> lines = text.getLines();

        if (lines == null || lines.isEmpty()) {
            return;
        }

        FontMetrics fontMetrics = getFontMetrics();
        int lineCount = lines.size();

        int fontHeight = fontMetrics.getAscent();
        int between = fontMetrics.getLeading() + fontMetrics.getDescent();

        int totalHeight = (fontHeight * lines.size()) + between * (lineCount - 1);
        int emptyHeight = canvasDelegate.getHeight() - totalHeight;

        int translateY = fontHeight + (emptyHeight / 2) - paddingY;

        int width = canvasDelegate.getWidth();

        for (String line : lines) {
            int lineWidth = fontMetrics.stringWidth(line);
            int x = (width - lineWidth) / 2;
            int y = translateY;

            translateY += fontHeight;
            lineCount--;

            if (lineCount > 0) {
                translateY += between;
            }

            drawLines.add(new StringWithPosition(line, x, y));
        }
    }

    private int getFreeWidth() {
        return canvasDelegate.getWidth() - 2 * paddingX;
    }

    private int getFreeHeight() {
        return canvasDelegate.getHeight() - 2 * paddingY;
    }

    public WrapperFactory getWrapperFactory() {
        return new WrapperFactory(getFreeWidth(), getFreeHeight(), getFontMetrics());
    }

    private void onFactoryChange() {
        if (factoryChangeListener != null) {
            factoryChangeListener.onWrapperFactoryChanged(getWrapperFactory());
        }
    }

    public TextWrapperFactoryChangeListener getWrapperChangeListener() {
        return factoryChangeListener;
    }

    public void setWrapperChangeListener(TextWrapperFactoryChangeListener factoryChangeListener) {
        this.factoryChangeListener = factoryChangeListener;
        factoryChangeListener.onWrapperFactoryChanged(getWrapperFactory());
    }

    private FontMetrics getFontMetrics() {
        return fontLabel.getFontMetrics(fontLabel.getFont());
    }
}
