/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import dev.juhouse.projector.projection2.models.BackgroundModel;
import dev.juhouse.projector.other.ProjectorPreferences;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 *
 * @author guilherme
 */
public class ProjectionBackground extends ProjectionImage {
    
    ProjectionBackground(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);

        this.setModel(getCanvasDelegate().getSettingsService().getLastBackground());
        this.setCropBackground(ProjectorPreferences.getCropBackground());
    }

    private CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    public void setModel(BackgroundModel model) {
        super.setModel(model);
        getCanvasDelegate().getSettingsService().storeLastBackground(model);
    }

    protected void update() {
        if (getRender()) {
            BufferedImage image = getModel().getStaticBackground();

            canvasDelegate.getBridge().setImageBackgroundAsset(
                    ((DataBufferInt) image.getRaster().getDataBuffer()).getData(),
                    image.getWidth(),
                    image.getHeight(),
                    getCropBackground()
            );
        } else {
            canvasDelegate.getBridge().setImageBackgroundAsset(null, 0, 0, getCropBackground());
        }
    }
}
