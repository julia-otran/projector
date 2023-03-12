package dev.juhouse.projector.projection2;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;

import dev.juhouse.projector.projection2.models.StringWithPosition;
import dev.juhouse.projector.projection2.text.WrappedText;
import dev.juhouse.projector.projection2.text.WrapperFactory;
import dev.juhouse.projector.utils.FontCreatorUtil;
import javafx.application.Platform;
import dev.juhouse.projector.other.ProjectorPreferences;

import javax.swing.border.StrokeBorder;

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

    private final Map<Integer, BufferedImage> renderImages = new HashMap<>();
    private final Map<Integer, Graphics2D> graphics = new HashMap<>();

    private Graphics2D defaultGraphics;

    private final Color clearColor;

    private BridgeRender[] renders;

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
        this.renders = canvasDelegate.getBridge().getRenderSettings();

        for (BridgeRender render : renders) {
            BufferedImage img = new BufferedImage(render.getTextAreaWidth(), render.getTextAreaHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            renderImages.put(render.getRenderId(), img);
            graphics.put(render.getRenderId(), g);

            if (render.getRenderMode() == 1) {
                defaultGraphics = img.createGraphics();
            }
        }
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

        if (defaultGraphics != null) {
            this.fontMetrics = defaultGraphics.getFontMetrics(font);
            onFactoryChange();
        }

        ProjectorPreferences.setProjectionLabelFontName(font.getFamily());
        ProjectorPreferences.setProjectionLabelFontStyle(font.getStyle());
        ProjectorPreferences.setProjectionLabelFontSize(font.getSize());
    }

    public void setText(WrappedText text) {
        this.text = text;
        generateLines();
    }

    private void renderText() {
        if (getFont() == null || drawLines.isEmpty()) {
            canvasDelegate.getBridge().setTextData(null);
            return;
        }

        BridgeTextData[] textData = new BridgeTextData[renders.length];

        for (int i = 0; i < renders.length; i++) {
            BridgeRender render = renders[i];

            Graphics2D g = graphics.get(render.getRenderId());

            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.Clear);
            g.setColor(clearColor);
            g.fillRect(0, 0, render.getWidth(), render.getHeight());
            g.setComposite(oldComposite);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float scaleX = render.getTextAreaWidth() / (float)canvasDelegate.getTextWidth();
            float scaleY = render.getTextAreaHeight() / (float)canvasDelegate.getTextHeight();
            float newFontSize = getFont().getSize() * Math.min(scaleX, scaleY);

            g.setStroke(new BasicStroke(3f + (newFontSize * 0.025f)));

            AffineTransform baseTransform = g.getTransform();

            drawLines.forEach((pt) -> {
                g.translate(pt.getX() * scaleX, pt.getY() * scaleY);

                // create a glyph vector from your text
                GlyphVector glyphVectorOutline = getFont()
                        .deriveFont(newFontSize)
                        .createGlyphVector(g.getFontRenderContext(), pt.getText());

                Shape textShape = glyphVectorOutline.getOutline();

                g.setColor(Color.black);
                g.draw(textShape);

                g.setColor(Color.white);
                g.fill(textShape);

                g.setTransform(baseTransform);
            });

            BufferedImage img = renderImages.get(render.getRenderId());
            int[] imgData = ((DataBufferInt)img.getData().getDataBuffer()).getData();

            textData[i] = new BridgeTextData(
                    render.getRenderId(),
                    imgData,
                    img.getWidth(),
                    img.getHeight(),
                    drawLines.stream().map(StringWithPosition::getX).min(Integer::compareTo).orElse(0) * scaleX,
                    drawLines.stream().map(StringWithPosition::getY).min(Integer::compareTo).orElse(0) * scaleY,
                    drawLines.stream().map(StringWithPosition::getW).max(Integer::compareTo).orElse(0) * scaleX,
                    drawLines.stream().map(s -> s.getY() + s.getH()).max(Integer::compareTo).orElse(0) * scaleY
                    );
        }

        canvasDelegate.getBridge().setTextData(textData);
    }

    private void generateLines() {
        if (text == null) {
            drawLines = Collections.emptyList();
            renderText();
            return;
        }

        if (font == null) {
            renderText();
            return;
        }

        List<String> lines = text.getLines();

        if (lines == null || text.isEmpty() || lines.isEmpty()) {
            drawLines = Collections.emptyList();
            renderText();
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

            pendingLines.add(new StringWithPosition(x, y, lineWidth, y - translateY, line));
        }

        if (pendingLines.stream().allMatch(l -> l.getText().isEmpty())) {
            drawLines = Collections.emptyList();
            renderText();
            return;
        }

        drawLines = pendingLines;

        renderText();
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
