/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection;

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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import dev.juhouse.projector.projection.models.StringWithPosition;
import dev.juhouse.projector.projection.models.VirtualScreen;
import dev.juhouse.projector.projection.text.WrappedText;
import dev.juhouse.projector.projection.text.WrapperFactory;
import dev.juhouse.projector.utils.FontCreatorUtil;
import javafx.application.Platform;
import lombok.Getter;
import dev.juhouse.projector.other.ProjectorPreferences;

/**
 *
 * @author guilherme
 */
public class ProjectionLabel implements Projectable {

    private final CanvasDelegate canvasDelegate;

    private static final int DEFAULT_PADDING_X = 120;
    private static final int DEFAULT_PADDING_Y = 40;
    private static final float STROKE_RATIO = 0.05f;

    // Label used to get fontMetrics
    private Font font;
    private FontMetrics fontMetrics;

    // Customizations
    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;
    private WrappedText text;
    private final int paddingY = DEFAULT_PADDING_Y;

    // Internal control
    private List<StringWithPosition> drawLines = Collections.emptyList();

    private BasicStroke outlineStroke;
    private BasicStroke chromaStroke;

    private final HashMap<String, PaintableCrossFader> faders = new HashMap<>();

    private final HashMap<String, AffineTransform> chromaScreenTransforms = new HashMap<>();

    public boolean getHasText() {
        return !drawLines.isEmpty();
    }

    @Getter
    private int chromaFontSize;

    @Getter
    private int chromaPaddingBottom;

    @Getter
    private int chromaMinPaddingBottom;

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        chromaFontSize = ProjectorPreferences.getChromaFontSize();
        chromaPaddingBottom = ProjectorPreferences.getChromaPaddingBottom();
        chromaMinPaddingBottom = ProjectorPreferences.getChromaMinPaddingBottom();

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

        rebuildLayout();
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
        this.outlineStroke = new BasicStroke(font.getSize() * STROKE_RATIO);

        onFactoryChange();

        ProjectorPreferences.setProjectionLabelFontName(font.getFamily());
        ProjectorPreferences.setProjectionLabelFontStyle(font.getStyle());
        ProjectorPreferences.setProjectionLabelFontSize(font.getSize());
    }

    public void setChromaFontSize(int fontSize) {
        this.chromaFontSize = fontSize;
        this.chromaStroke = new BasicStroke(fontSize * STROKE_RATIO);
        ProjectorPreferences.setChromaFontSize(fontSize);
        generateLines();
    }

    public void setChromaPaddingBottom(int paddingBottom) {
        this.chromaPaddingBottom = paddingBottom;
        ProjectorPreferences.setChromaPaddingBottom(paddingBottom);
        generateLines();
    }

    public void setChromaMinPaddingBottom(int minPaddingBottom) {
        this.chromaMinPaddingBottom = minPaddingBottom;
        ProjectorPreferences.setChromaMinPaddingBottom(minPaddingBottom);
        generateLines();
    }

    public void setText(WrappedText text) {
        this.text = text;
        generateLines();
    }

    private void renderText() {
        canvasDelegate.getVirtualScreens().forEach(vs -> {
            BufferedImage newImage = new BufferedImage(vs.getWidth(), vs.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = newImage.createGraphics();

            if (!drawLines.isEmpty()) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (vs.isChromaScreen()) {
                    g.setStroke(chromaStroke);
                } else {
                    g.setStroke(outlineStroke);
                }

                AffineTransform placement = chromaScreenTransforms.get(vs.getVirtualScreenId());

                if (placement != null) {
                    AffineTransform preTransform = g.getTransform();
                    preTransform.concatenate(placement);
                    g.setTransform(preTransform);
                }

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

            g.dispose();

            PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

            if (fader != null) {
                fader.crossFadeIn(new ImagePaintable(newImage));
            }
        });
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

        if (fader != null) {
            fader.paintComponent(g);
        }
    }

    private void fadeOut() {
        faders.forEach((screenId, fader) -> fader.fadeOut());
    }

    @Override
    public void rebuildLayout() {
        faders.clear();

        canvasDelegate.getVirtualScreens().forEach(vs -> {
            PaintableCrossFader fader = new PaintableCrossFader(vs);
            fader.setStepPerFrame(0.1f);
            faders.put(vs.getVirtualScreenId(), fader);
        });

        generateLines();
    }

    private void generateLines() {
        if (text == null) {
            drawLines = Collections.emptyList();
            fadeOut();
            return;
        }

        if (font == null) {
            fadeOut();
            return;
        }

        List<String> lines = text.getLines();

        if (lines == null || text.isEmpty() || lines.isEmpty()) {
            drawLines = Collections.emptyList();
            fadeOut();
            return;
        }

        FontMetrics fontMetrics = getFontMetrics();
        int lineCount = lines.size();

        int fontHeight = fontMetrics.getAscent();
        int between = fontMetrics.getLeading() + fontMetrics.getDescent();

        int totalHeight = (fontHeight * lines.size()) + between * (lineCount - 1);
        int emptyHeight = canvasDelegate.getMainHeight() - totalHeight;

        int translateY = fontHeight + (emptyHeight / 2) - paddingY;

        int width = canvasDelegate.getMainWidth();

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
            fadeOut();
            return;
        }

        drawLines = pendingLines;

        float chromaFontScale = chromaFontSize / (float) getFont().getSize();

        canvasDelegate.getVirtualScreens().forEach(vs -> {
            AffineTransform chromaScreenTransform = new AffineTransform();

            if (vs.isChromaScreen()) {
                int chromaTranslateX = (vs.getWidth() - Math.round(canvasDelegate.getMainWidth() * chromaFontScale)) / 2;

                int bottomBlankSpace = Math.round(emptyHeight * chromaFontScale / 2) + chromaPaddingBottom;
                int chromaTranslateY = vs.getHeight() - bottomBlankSpace;

                int cropCheck = chromaTranslateY +
                        Math.round(totalHeight * chromaFontScale) +
                        Math.round(emptyHeight * chromaFontScale / 2) +
                        chromaMinPaddingBottom;

                if (cropCheck > vs.getHeight()) {
                    int delta = cropCheck - vs.getHeight();
                    chromaTranslateY -= delta;
                }

                chromaScreenTransform.translate(chromaTranslateX, chromaTranslateY);
                chromaScreenTransform.scale(chromaFontScale, chromaFontScale);
            }

            chromaScreenTransforms.put(vs.getVirtualScreenId(), chromaScreenTransform);
        });

        renderText();
    }

    private int getFreeWidth() {
        return canvasDelegate.getMainWidth() - 2 * DEFAULT_PADDING_X;
    }

    private int getFreeHeight() {
        return canvasDelegate.getMainHeight() - 2 * paddingY;
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

    static class ImagePaintable implements Paintable {
        private final BufferedImage image;

        ImagePaintable(BufferedImage image) {
            this.image = image;
        }

        @Override
        public void paintComponent(Graphics2D g, VirtualScreen vs) {
            g.drawImage(image, 0, 0, null);
        }
    }
}
