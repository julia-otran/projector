/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.image;

import dev.juhouse.projector.projection2.BridgeRender;
import dev.juhouse.projector.projection2.CanvasDelegate;
import dev.juhouse.projector.projection2.image.ProjectionImage;
import dev.juhouse.projector.projection2.models.BackgroundModel;
import dev.juhouse.projector.other.ProjectorPreferences;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionBackground extends ProjectionImage {

    public ProjectionBackground(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);
    }

    @Override
    public void init() {
        super.init();
        this.setModel(getCanvasDelegate().getSettingsService().getLastBackground());
        this.setCropBackground(ProjectorPreferences.getCropBackground());
        this.setRender(true);
    }

    @Override
    public void rebuild() {
        super.rebuild();
        this.getRenderFlagProperty().get().applyDefault(BridgeRender::getEnableRenderBackgroundAssets);
    }

    private CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    public void setModel(BackgroundModel model) {
        super.setModel(model);
        getCanvasDelegate().getSettingsService().storeLastBackground(model);
    }
}
