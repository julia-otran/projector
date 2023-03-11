/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.juhouse.projector.projection2.models.StringWithPosition;
import dev.juhouse.projector.projection2.text.WrappedText;
import dev.juhouse.projector.projection2.text.WrapperFactory;
import dev.juhouse.projector.utils.FontCreatorUtil;
import javafx.application.Platform;
import dev.juhouse.projector.other.ProjectorPreferences;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionLabel implements Projectable {

    private final CanvasDelegate canvasDelegate;

    // Label used to get fontMetrics
    private Font font;
    private FontMetrics fontMetrics;

    // Customizations
    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;
    private WrappedText text;

    // Internal control
    private List<StringWithPosition> drawLines = Collections.emptyList();

    private BufferedImage image;
    private Graphics2D g;

    private final Color clearColor;

    private BasicStroke stroke;

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        clearColor = new Color(255, 255, 255, 0);
    }

    @Override
    public void init() {
        setFont(
                FontCreatorUtil.createFont(
                        ProjectorPreferences.getProjectionLabelFontName(),
                        ProjectorPreferences.getProjectionLabelFontStyle(),
                        ProjectorPreferences.getProjectionLabelFontSize()
                )
        );

        generateLines();
    }

    @Override
    public void finish() {

    }

    @Override
    public void rebuild() {
        image = new BufferedImage(canvasDelegate.getTextWidth(), canvasDelegate.getTextHeight(), BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
    }

    @Override
    public void setRender(boolean render) {
        // I think we will never prevent label rendering
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        this.fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);

        stroke = new BasicStroke(font.getSize() * 0.05f);

        onFactoryChange();

        ProjectorPreferences.setProjectionLabelFontName(font.getFamily());
        ProjectorPreferences.setProjectionLabelFontStyle(font.getStyle());
        ProjectorPreferences.setProjectionLabelFontSize(font.getSize());
    }

    public void setText(WrappedText text) {
        this.text = text;
        generateLines();
    }

    private void renderText(int textHeight) {
        if (getFont() == null || drawLines.isEmpty()) {
            canvasDelegate.getBridge().setTextImage(null, 0);
            return;
        }

        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.Clear);
        g.setColor(clearColor);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setComposite(oldComposite);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setStroke(stroke);

        AffineTransform oldTransform = g.getTransform();

        drawLines.forEach((pt) -> {
            g.translate(pt.getX(), pt.getY());
            // create a glyph vector from your text
            GlyphVector glyphVector = getFont().createGlyphVector(g.getFontRenderContext(), pt.getText());

            // get the shape object
            Shape textShape = glyphVector.getOutline();

            g.setColor(Color.white);
            g.fill(textShape);

            g.setTransform(oldTransform);
        });

        canvasDelegate.getBridge().setTextImage(((DataBufferInt)image.getRaster().getDataBuffer()).getData(), textHeight);
    }

    private void generateLines() {
        if (text == null) {
            drawLines = Collections.emptyList();
            renderText(0);
            return;
        }

        if (font == null) {
            renderText( 0);
            return;
        }

        List<String> lines = text.getLines();

        if (lines == null || text.isEmpty() || lines.isEmpty()) {
            drawLines = Collections.emptyList();
            renderText( 0);
            return;
        }

        FontMetrics fontMetrics = getFontMetrics();
        int lineCount = lines.size();

        int fontHeight = fontMetrics.getAscent();
        int between = fontMetrics.getLeading() + fontMetrics.getDescent();

        int totalHeight = (fontHeight * lines.size()) + between * (lineCount - 1);
        int emptyHeight = canvasDelegate.getTextHeight() - totalHeight;

        int translateY = fontHeight + (emptyHeight / 2);

        int width = canvasDelegate.getTextWidth();

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
            drawLines = Collections.emptyList();
            renderText( 0);
            return;
        }

        drawLines = pendingLines;

        renderText(totalHeight);
    }

    private int getFreeWidth() {
        return canvasDelegate.getMainWidth();
    }

    private int getFreeHeight() {
        return canvasDelegate.getMainHeight();
    }

    public WrapperFactory getWrapperFactory() {
        return new WrapperFactory(getFreeWidth(), getFreeHeight(), getFontMetrics());
    }

    private void onFactoryChange() {
        Platform.runLater(() -> factoryChangeListeners.forEach(l -> l.onWrapperFactoryChanged(getWrapperFactory())));
    }

    public void addWrapperChangeListener(TextWrapperFactoryChangeListener factoryChangeListener) {
        factoryChangeListeners.add(factoryChangeListener);
        factoryChangeListener.onWrapperFactoryChanged(getWrapperFactory());
    }

    private FontMetrics getFontMetrics() {
        return this.fontMetrics;
    }
}
