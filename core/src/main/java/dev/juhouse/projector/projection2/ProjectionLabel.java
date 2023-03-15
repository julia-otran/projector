package dev.juhouse.projector.projection2;

import java.awt.*;
import java.util.*;
import java.util.List;

import dev.juhouse.projector.projection2.text.*;
import dev.juhouse.projector.utils.FontCreatorUtil;
import javafx.application.Platform;
import dev.juhouse.projector.other.ProjectorPreferences;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionLabel implements Projectable {
    private final CanvasDelegate canvasDelegate;

    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;

    private BridgeRender[] bridgeRenders;
    private final Map<Integer, TextRenderer> textRenders;

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        textRenders = new HashMap<>();

        canvasDelegate.getFontProperty().addListener((prop, oldValue, newValue) -> {
            updateFont();
        });
    }

    @Override
    public void init() {
    }

    @Override
    public void finish() {
    }

    @Override
    public void rebuild() {
        this.textRenders.clear();
        this.bridgeRenders = canvasDelegate.getBridge().getRenderSettings();

        for (BridgeRender render : bridgeRenders) {
            TextRendererBounds bounds = new TextRendererBounds(
                    render.getRenderId(),
                    render.getTextAreaX(),
                    render.getTextAreaY(),
                    render.getTextAreaWidth(),
                    render.getTextAreaHeight());

            this.textRenders.put(render.getRenderId(), new TextRenderer(bounds, getFontFor(render.getRenderId())));
        }
    }

    @Override
    public void setRender(boolean render) {
        // I think we will never prevent label rendering
    }

    private void updateFont() {
        this.textRenders.values().forEach(tr -> tr.setFont(getFontFor(tr.getBounds().getRenderId())));
        onFactoryChange();
    }

    public Font getFontFor(int renderId) {
        for (BridgeRender render : bridgeRenders) {
            if (render.getRenderId() == renderId) {
                float scaleX = render.getTextAreaWidth() / (float) canvasDelegate.getTextWidth();
                float scaleY = render.getTextAreaHeight() / (float) canvasDelegate.getTextHeight();
                float newFontSize = canvasDelegate.getFontProperty().getValue().getSize() * Math.min(scaleX, scaleY);

                return canvasDelegate.getFontProperty().getValue().deriveFont(newFontSize);
            }
        }

        return canvasDelegate.getFontProperty().getValue();
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
        if (text == null || text.isEmpty()) {
            canvasDelegate.getBridge().setTextData(null);
            return;
        }

        final BridgeTextData[] textData = new BridgeTextData[bridgeRenders.length];

        text.renderLines().forEach((renderId, lines) -> textData[getRenderIndex(renderId)] = textRenders.get(renderId).renderText(lines));

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
