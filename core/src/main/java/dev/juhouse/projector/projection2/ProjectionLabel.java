package dev.juhouse.projector.projection2;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;

import dev.juhouse.projector.projection2.models.StringWithPosition;
import dev.juhouse.projector.projection2.text.TextRenderer;
import dev.juhouse.projector.projection2.text.TextWrapperMetrics;
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

    private Font font;

    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;

    private BridgeRender[] bridgeRenders;
    private Map<Integer, TextRenderer> textRenders;

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        textRenders = new HashMap<>();
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
    }

    @Override
    public void finish() {

    }

    @Override
    public void rebuild() {
        this.textRenders.clear();
        this.bridgeRenders = canvasDelegate.getBridge().getRenderSettings();

        for (BridgeRender render : bridgeRenders) {
            this.textRenders.put(render.getRenderId(), new TextRenderer(render, getFontFor(render.getRenderId())));
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
        this.textRenders.values().forEach(tr -> tr.setFont(getFontFor(tr.getRender().getRenderId())));

        onFactoryChange();

        ProjectorPreferences.setProjectionLabelFontName(font.getFamily());
        ProjectorPreferences.setProjectionLabelFontStyle(font.getStyle());
        ProjectorPreferences.setProjectionLabelFontSize(font.getSize());
    }

    public Font getFontFor(int renderId) {
        for (int i = 0; i < bridgeRenders.length; i++) {
            BridgeRender render = bridgeRenders[i];

            if (render.getRenderId() == renderId) {
                float scaleX = render.getTextAreaWidth() / (float)canvasDelegate.getTextWidth();
                float scaleY = render.getTextAreaHeight() / (float)canvasDelegate.getTextHeight();
                float newFontSize = getFont().getSize() * Math.min(scaleX, scaleY);

                return getFont().deriveFont(newFontSize);
            }
        }

        return getFont();
    }

    private int getRenderIndex(int renderId) {
        for (int i = 0; i < bridgeRenders.length; i++) {
            if (bridgeRenders[i].getRenderId() == renderId) {
                return i;
            }
        }

        return 0;
    }

    public void setText(WrappedText text) {
        if (getFont() == null || text == null || text.isEmpty()) {
            canvasDelegate.getBridge().setTextData(null);
            return;
        }

        final BridgeTextData[] textData = new BridgeTextData[bridgeRenders.length];

        text.renderLines().forEach((renderId, lines) -> {
            textData[getRenderIndex(renderId)] = textRenders.get(renderId).renderText(lines);
        });

        canvasDelegate.getBridge().setTextData(textData);
    }

    public WrapperFactory getWrapperFactory() {
        return new WrapperFactory(textRenders.values().stream().map(TextRenderer::getTextWrapperMetrics).toList());
    }

    private void onFactoryChange() {
        Platform.runLater(() -> factoryChangeListeners.forEach(l -> l.onWrapperFactoryChanged(getWrapperFactory())));
    }

    public void addWrapperChangeListener(TextWrapperFactoryChangeListener factoryChangeListener) {
        factoryChangeListeners.add(factoryChangeListener);
        factoryChangeListener.onWrapperFactoryChanged(getWrapperFactory());
    }
}
