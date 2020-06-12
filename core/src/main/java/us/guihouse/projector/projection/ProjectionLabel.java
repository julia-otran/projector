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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javafx.application.Platform;
import lombok.Getter;
import us.guihouse.projector.other.ProjectorPreferences;
import us.guihouse.projector.projection.models.StringWithPosition;
import us.guihouse.projector.projection.models.VirtualScreen;
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
    private static final int CHROMA_PADDING_BOTTOM = 160;
    private static final int CHROMA_PADDING_BOTTOM_MIN = 64;

    private static final float CHROMA_OUTPUT_SCALE = 0.4f;

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
    private boolean darkenBackground;

    // Internal control
    private List<StringWithPosition> drawLines = Collections.emptyList();

    private final BasicStroke outlineStroke = new BasicStroke(8.0f);
    private static final Color OVERLAY = new Color(0, 0, 0, 230);

    private final HashMap<String, PaintableCrossFader> faders = new HashMap<>();

    private final HashMap<String, AffineTransform> chromaScreenTransforms = new HashMap<>();

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        darkenBackground = ProjectorPreferences.getDarkenBackground();
    }

    @Override
    public void init() {
        setFont(new java.awt.Font(Font.SANS_SERIF, 0, DEFAULT_FONT_SIZE));
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
        onFactoryChange();
    }

    public void setDarkenBackground(boolean darken) {
        this.darkenBackground = darken;
        ProjectorPreferences.setDarkenBackground(darken);
        renderText();
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
        generateLines();
    }

    private void renderText() {
        canvasDelegate.getVirtualScreens().forEach(vs -> {
            BufferedImage newImage = new BufferedImage(vs.getWidth(), vs.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = newImage.createGraphics();

            if (!drawLines.isEmpty()) {
                if (darkenBackground && !vs.isChromaScreen()) {
                    g.setColor(OVERLAY);
                    g.fillRect(0, 0, canvasDelegate.getMainWidth(), canvasDelegate.getMainHeight());
                }

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setStroke(outlineStroke);

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

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
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

        canvasDelegate.getVirtualScreens().forEach(vs -> {
            AffineTransform chromaScreenTransform = new AffineTransform();

            if (vs.isChromaScreen()) {
                int chromaTranslateX = (vs.getWidth() - Math.round(canvasDelegate.getMainWidth() * CHROMA_OUTPUT_SCALE)) / 2;

                int bottomBlankSpace = Math.round(emptyHeight * CHROMA_OUTPUT_SCALE / 2) + Math.round(CHROMA_PADDING_BOTTOM / 900f * vs.getHeight());
                int chromaTranslateY = vs.getHeight() - bottomBlankSpace;

                int cropCheck = chromaTranslateY +
                        Math.round(totalHeight * CHROMA_OUTPUT_SCALE) +
                        Math.round(emptyHeight * CHROMA_OUTPUT_SCALE / 2) +
                        Math.round(CHROMA_PADDING_BOTTOM_MIN / 900f * vs.getHeight());

                if (cropCheck > vs.getHeight()) {
                    int delta = cropCheck - vs.getHeight();
                    chromaTranslateY -= delta;
                }

                chromaScreenTransform.translate(chromaTranslateX, chromaTranslateY);
                chromaScreenTransform.scale(CHROMA_OUTPUT_SCALE, CHROMA_OUTPUT_SCALE);
            }

            chromaScreenTransforms.put(vs.getVirtualScreenId(), chromaScreenTransform);
        });

        renderText();
    }

    private int getFreeWidth() {
        return canvasDelegate.getMainWidth() - 2 * paddingX;
    }

    private int getFreeHeight() {
        return canvasDelegate.getMainHeight() - 2 * paddingY;
    }

    public WrapperFactory getWrapperFactory() {
        return new WrapperFactory(getFreeWidth(), getFreeHeight(), getFontMetrics());
    }

    private void onFactoryChange() {
        Platform.runLater(() -> {
            factoryChangeListeners.forEach(l -> l.onWrapperFactoryChanged(getWrapperFactory()));
        });
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
