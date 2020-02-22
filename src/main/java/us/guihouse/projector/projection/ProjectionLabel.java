/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.other.ProjectorPreferences;
import us.guihouse.projector.projection.models.StringWithPosition;
import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.projection.text.WrapperFactory;

/**
 *
 * @author guilherme
 */
public class ProjectionLabel implements Projectable {

    private final CanvasDelegate canvasDelegate;

    private static final int DEFAULT_PADDING_X = 120;
    private static final int DEFAULT_PADDING_Y = 40;

    private static final int DEFAULT_FONT_SIZE = 112;

    // Label used to get fontMetrics
    private Font font;
    private FontMetrics fontMetrics;

    // Customizations
    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;
    private WrappedText text;
    private int paddingX = DEFAULT_PADDING_X;
    private int paddingY = DEFAULT_PADDING_Y;

    @Getter
    @Setter
    private boolean darkenBackground;

    // Internal control
    private List<StringWithPosition> drawLines = Collections.EMPTY_LIST;

    private final BasicStroke outlineStroke = new BasicStroke(8.0f);
    private static final Color OVERLAY = new Color(0, 0, 0, 230);

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        darkenBackground = ProjectorPreferences.getDarkenBackground();
    }

    @Override
    public void init() {
        setFont(new java.awt.Font(Font.SANS_SERIF, 0, DEFAULT_FONT_SIZE));
    }

    @Override
    public void finish() {

    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        this.fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
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
    }

    @Override
    public void paintComponent(Graphics2D g) {
        if (drawLines.isEmpty()) {
            return;
        }

        if (darkenBackground) {
            g.setColor(OVERLAY);
            g.fillRect(0, 0, canvasDelegate.getWidth(), canvasDelegate.getHeight());
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setStroke(outlineStroke);

        AffineTransform oldTransform = g.getTransform();

        drawLines.forEach((pt) -> {
            g.translate(pt.getX(), pt.getY());
            // create a glyph vector from your text
            GlyphVector glyphVector = getFont().createGlyphVector(g.getFontRenderContext(), pt.getText());

            // get the shape object
            Shape textShape = glyphVector.getOutline();

            g.setColor(Color.black);
            g.draw(textShape);

            g.setColor(Color.white);
            g.fill(textShape);

            g.setTransform(oldTransform);
        });
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    @Override
    public void rebuildLayout() {
        if (text == null) {
            drawLines = Collections.EMPTY_LIST;
            return;
        }

        if (font == null) {
            return;
        }

        List<String> lines = text.getLines();

        if (lines == null || text.isEmpty() || lines.isEmpty()) {
            drawLines = Collections.EMPTY_LIST;
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

        List<StringWithPosition> pendingLines = new ArrayList<>();

        for (String line : lines) {
            int lineWidth = fontMetrics.stringWidth(line);
            int x = (width - lineWidth) / 2;
            int y = translateY;

            translateY += fontHeight;
            lineCount--;

            if (lineCount > 0) {
                translateY += between;
            }

            pendingLines.add(new StringWithPosition(line, x, y));
        }

        if (pendingLines.stream().allMatch(l -> l.getText().isEmpty())) {
            drawLines = Collections.EMPTY_LIST;
            return;
        }

        drawLines = pendingLines;
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
        factoryChangeListeners.forEach(l -> l.onWrapperFactoryChanged(getWrapperFactory()));
    }

    public void addWrapperChangeListener(TextWrapperFactoryChangeListener factoryChangeListener) {
        factoryChangeListeners.add(factoryChangeListener);
        factoryChangeListener.onWrapperFactoryChanged(getWrapperFactory());
    }

    private FontMetrics getFontMetrics() {
        return this.fontMetrics;
    }
}
