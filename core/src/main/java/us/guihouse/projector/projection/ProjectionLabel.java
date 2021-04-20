/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.application.Platform;
import lombok.Getter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import us.guihouse.projector.other.ProjectorPreferences;
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapter;
import us.guihouse.projector.projection.glfw.RGBImageCopy;
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

    // Label used to get fontMetrics
    private Font font;
    private FontMetrics fontMetrics;

    // Customizations
    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;
    private WrappedText text;
    private final int paddingY = DEFAULT_PADDING_Y;

    // Internal control
    private List<StringWithPosition> drawLines = Collections.emptyList();

    private final BasicStroke outlineStroke = new BasicStroke(8.0f);

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
        setFont(new java.awt.Font(Font.SANS_SERIF, Font.PLAIN, ProjectorPreferences.getProjectionLabelFontSize()));
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
        ProjectorPreferences.setProjectionLabelFontSize(font.getSize());
    }

    public void setChromaFontSize(int fontSize) {
        this.chromaFontSize = fontSize;
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
            virtualScreenImages.put(vs, newImage);
        });
    }

    private final ConcurrentHashMap<VirtualScreen, BufferedImage> virtualScreenImages = new ConcurrentHashMap<>();

    @Override
    public void paintComponent(GLFWGraphicsAdapter g, VirtualScreen vs) {
        BufferedImage img = virtualScreenImages.remove(vs);
        PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

        if (img != null && fader != null) {
            int tex = g.getProvider().dequeueTex();

            int buffer = GL30.glGenBuffers();
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer);
            GL30.glBufferData(
                    GL30.GL_PIXEL_UNPACK_BUFFER,
                    img.getWidth() * img.getHeight() * 4L,
                    GL30.GL_STREAM_DRAW
            );

            ByteBuffer destination = GL30.glMapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, GL30.GL_WRITE_ONLY);

            if (destination != null) {
                RGBImageCopy.copyImageToBuffer(img, destination, true);
            }

            GL30.glUnmapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            //ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
            // RGBImageCopy.copyImageToBuffer(img, buffer, true);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, img.getWidth(), img.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);

            fader.crossFadeIn(new TexPaintable(tex), () ->
                    g.getProvider().enqueueForDraw(() ->
                        g.getProvider().freeTex(tex)
                    )
            );
        }

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

    static class TexPaintable implements Paintable {
        private final int tex;

         private TexPaintable(int tex) {
            this.tex = tex;
        }

        @Override
        public void paintComponent(GLFWGraphicsAdapter g, VirtualScreen vs) {
            float alpha = g.getAlpha();

            g.getProvider().enqueueForDraw(() -> {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                GL11.glPushMatrix();
                g.adjustOrtho();
                g.updateAlpha(alpha);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

                GL11.glBegin(GL11.GL_QUADS);

                GL11.glTexCoord2d(0, 0);
                GL11.glVertex2i(0, 0);

                GL11.glTexCoord2d(0, 1);
                GL11.glVertex2i(0, vs.getHeight());

                GL11.glTexCoord2d(1, 1);
                GL11.glVertex2i(vs.getWidth(), vs.getHeight());

                GL11.glTexCoord2d(1, 0);
                GL11.glVertex2i(vs.getWidth(), 0);

                GL11.glEnd();

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                GL11.glPopMatrix();
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            });
        }
    }
}
