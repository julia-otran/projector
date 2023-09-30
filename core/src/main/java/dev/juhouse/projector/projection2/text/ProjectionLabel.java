package dev.juhouse.projector.projection2.text;

import java.awt.*;
import java.util.*;
import java.util.List;

import dev.juhouse.projector.projection2.*;
import javafx.application.Platform;
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
        private WrappedText current;
        private WrappedText ahead;
    }

    private final CanvasDelegate canvasDelegate;

    private final List<TextWrapperFactoryChangeListener> factoryChangeListeners;

    private BridgeRender[] bridgeRenders;
    private final Map<Integer, TextRenderer> textRenders;

    private final BridgeRenderFlag renderFlag;

    private MultipleWrappedText currentText;

    private boolean clear;

    public ProjectionLabel(CanvasDelegate canvasDelegate) {
        renderFlag = new BridgeRenderFlag(canvasDelegate);
        renderFlag.renderToAll();

        this.canvasDelegate = canvasDelegate;
        factoryChangeListeners = new ArrayList<>();
        textRenders = new HashMap<>();
    }

    @Override
    public void init() {
        canvasDelegate.getFontProperty().addListener((prop, oldValue, newValue) -> updateFont());

        renderFlag.getProperty().addListener((observableValue, number, t1) -> {
            boolean changed = false;

            for (TextRenderer textRender : textRenders.values()) {
                boolean enable = renderFlag.isRenderEnabled(textRender.getBounds().getRenderId());

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

            this.textRenders.put(render.getRenderId(), textRender);
        }

        setClear(clear);
        Platform.runLater(this::onFactoryChange);
    }

    @Override
    public void setRender(boolean render) {
        // I think we will never prevent label rendering
    }

    @Override
    public BridgeRenderFlag getRenderFlag() {
        return renderFlag;
    }

    private void updateFont() {
        this.textRenders.values().forEach(tr -> tr.setFont(getFontFor(tr.getBounds().getRenderId())));
        Platform.runLater(this::onFactoryChange);
    }

    public Font getFontFor(int renderId) {
        for (BridgeRender render : bridgeRenders) {
            if (render.getRenderId() == renderId) {
                float newFontSize = canvasDelegate.getFontProperty().getValue().getSize() * (float) render.getTextScale();
                int style = render.getEnableRenderTextBehindAndAhead() ? Font.BOLD : Font.PLAIN;
                return canvasDelegate.getFontProperty().getValue().deriveFont(style, newFontSize);
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

    public void setClear(boolean clear) {
        for (BridgeRender render : bridgeRenders) {
            TextRenderer textRender = textRenders.get(render.getRenderId());

            if (clear) {
                if (render.getEnableRenderTextBehindAndAhead()) {
                    textRender.setEnabled(renderFlag.isRenderEnabled(render.getRenderId()));
                } else {
                    textRender.setEnabled(false);
                }
            } else {
                textRender.setEnabled(renderFlag.isRenderEnabled(render.getRenderId()));
            }
        }

        if (this.clear != clear) {
            this.clear = clear;
            doRender();
        }
    }

    public void setText(WrappedText current, WrappedText ahead) {
        if (current == null || current.isEmpty()) {
            currentText = null;
        } else {
            setClear(false);
            currentText = new MultipleWrappedText(
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
            List<String> aheadLines = currentText.ahead.renderLines().get(renderId);

            if (aheadLines == null) {
                aheadLines = Collections.emptyList();
            }

            textData[getRenderIndex(renderId)] = tr.renderText(currentLines, aheadLines);
        });

        canvasDelegate.getBridge().setTextData(textData);
    }

    public WrapperFactory getWrapperFactory() {
        return new WrapperFactory(textRenders.values().stream().map(TextRenderer::getTextWrapperMetrics).toList());
    }

    private void onFactoryChange() {
        factoryChangeListeners.forEach(l -> l.onWrapperFactoryChanged(getWrapperFactory()));
    }

    public void addWrapperChangeListener(TextWrapperFactoryChangeListener factoryChangeListener) {
        factoryChangeListeners.add(factoryChangeListener);
        factoryChangeListener.onWrapperFactoryChanged(getWrapperFactory());
    }
}
