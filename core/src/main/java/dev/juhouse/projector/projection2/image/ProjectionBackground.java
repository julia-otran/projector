/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.image;

import dev.juhouse.projector.projection2.BridgeRender;
import dev.juhouse.projector.projection2.BridgeRenderFlag;
import dev.juhouse.projector.projection2.CanvasDelegate;
import dev.juhouse.projector.projection2.models.BackgroundModel;
import dev.juhouse.projector.other.ProjectorPreferences;
import javafx.beans.value.ChangeListener;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionBackground extends ProjectionBaseImage {
    private BridgeRenderFlag exclusionFlag;
    private BridgeRenderFlag availableFlag;

    private final ChangeListener<Number> exclusionFlagListener = (prop, oldValue, newValue) -> {
        updateRenderFlag();
    };

    public ProjectionBackground(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);
    }

    @Override
    public void init() {
        super.init();

        this.setCropBackground(ProjectorPreferences.getCropBackground());

        availableFlag = new BridgeRenderFlag(getCanvasDelegate());
        availableFlag.getProperty().addListener(exclusionFlagListener);

        this.setCropBackground(ProjectorPreferences.getCropBackground());
        this.setModel(getCanvasDelegate().getSettingsService().getLastBackground());

        getRenderFlag().renderToNone();

        this.setRender(true);
    }

    private void updateRenderFlag() {
        if (exclusionFlag == null) {
            getRenderFlag().setValue(availableFlag.getValue());
        } else {
            getRenderFlag().setValue(availableFlag.exclude(exclusionFlag));
        }
    }

    @Override
    public void rebuild() {
        availableFlag.applyDefault(BridgeRender::getEnableRenderBackgroundAssets);
        super.rebuild();
    }

    public void setModel(BackgroundModel model) {
        super.setModel(model);
        getCanvasDelegate().getSettingsService().storeLastBackground(model);
    }

    public void setExcludeRenderFlag(BridgeRenderFlag exclude) {
        if (this.exclusionFlag != null) {
            this.exclusionFlag.getProperty().removeListener(exclusionFlagListener);
        }

        this.exclusionFlag = exclude;

        if (exclude != null) {
            this.exclusionFlag.getProperty().addListener(exclusionFlagListener);
        }

        updateRenderFlag();
    }
}
