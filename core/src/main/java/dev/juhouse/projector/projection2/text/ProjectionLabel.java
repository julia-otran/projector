package dev.juhouse.projector.projection2.text;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import dev.juhouse.projector.projection2.*;
import dev.juhouse.projector.projection2.text.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionLabel implements Projectable {
    @Data
    @AllArgsConstructor
    private static class MultipleWrappedText {
        private WrappedText behind;
        private WrappedText current;
        private WrappedText ahead;
    }

    private final CanvasDelegate canvasDelegate;

    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;

    private BridgeRender[] bridgeRenders;
    private final Map<Integer, TextRenderer> textRenders;

    private final ReadOnlyObjectWrapper<BridgeRenderFlag> renderFlagProperty = new ReadOnlyObjectWrapper<>();

    private MultipleWrappedText currentText;

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        renderFlagProperty.set(new BridgeRenderFlag(canvasDelegate));
        renderFlagProperty.get().renderToAll();

        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        textRenders = new HashMap<>();
    }

    @Override
    public void init() {
        canvasDelegate.getFontProperty().addListener((prop, oldValue, newValue) -> updateFont());

        renderFlagProperty.get().getFlagValueProperty().addListener((observableValue, number, t1) -> {
            boolean changed = false;

            for (TextRenderer textRender : textRenders.values()) {
                boolean enable = renderFlagProperty.get().isRenderEnabled(textRender.getBounds().getRenderId());

                if (enable != textRender.getEnabled()) {
                    textRender.setEnabled(enable);
                    changed = true;
                }
            }

            if (changed) {
                doRender();
            }
        });
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
                    render.getTextAreaHeight(),
                    render.getEnableRenderTextBehindAndAhead());

            TextRenderer textRender = new TextRenderer(bounds, getFontFor(render.getRenderId()));

            textRender.setEnabled(renderFlagProperty.get().isRenderEnabled(render.getRenderId()));

            this.textRenders.put(render.getRenderId(), textRender);
        }

        onFactoryChange();
        doRender();
    }

    @Override
    public void setRender(boolean render) {
        // I think we will never prevent label rendering
    }

    @Override
    public ReadOnlyObjectProperty<BridgeRenderFlag> getRenderFlagProperty() {
        return renderFlagProperty.getReadOnlyProperty();
    }

    private void updateFont() {
        this.textRenders.values().forEach(tr -> tr.setFont(getFontFor(tr.getBounds().getRenderId())));
        onFactoryChange();
        doRender();
    }

    public Font getFontFor(int renderId) {
        for (BridgeRender render : bridgeRenders) {
            if (render.getRenderId() == renderId) {
                float newFontSize = canvasDelegate.getFontProperty().getValue().getSize() * (float) render.getTextScale();
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

    public void setText(WrappedText behind, WrappedText current, WrappedText ahead) {
        if (current == null || current.isEmpty()) {
            currentText = null;
        } else {
            currentText = new MultipleWrappedText(
                    behind == null ? WrappedText.blankText() : behind,
                    current,
                    ahead == null ? WrappedText.blankText() : ahead
            );
        }

        doRender();
    }

    private void doRender() {
        if (currentText == null) {
            canvasDelegate.getBridge().setTextData(null);
            return;
        }

        final BridgeTextData[] textData = new BridgeTextData[bridgeRenders.length];

        textRenders.forEach((renderId, tr) -> {
            List<String> currentLines = currentText.current.renderLines().get(renderId);
            List<String> behindLines = currentText.behind.renderLines().get(renderId);
            List<String> aheadLines = currentText.ahead.renderLines().get(renderId);

            if (behindLines == null) {
                behindLines = Collections.emptyList();
            }

            if (aheadLines == null) {
                aheadLines = Collections.emptyList();
            }

            textData[getRenderIndex(renderId)] = tr.renderText(behindLines, currentLines, aheadLines);
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
