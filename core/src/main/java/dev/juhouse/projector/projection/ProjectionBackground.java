/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection;

import dev.juhouse.projector.projection.models.BackgroundModel;
import dev.juhouse.projector.projection.models.VirtualScreen;
import dev.juhouse.projector.other.ProjectorPreferences;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author guilherme
 */
public class ProjectionBackground extends ProjectionImage {
    private final Color bgColor;
    private final Color chromaBgColor;
    
    ProjectionBackground(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);
        
        // Place some color to prevent monitor from sleeping
        bgColor = new Color(20, 20, 20);

        chromaBgColor = new Color(0, 255, 0);

        this.setModel(getCanvasDelegate().getSettingsService().getLastBackground());
        this.setCropBackground(ProjectorPreferences.getCropBackground());
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        if (vs.isChromaScreen()) {
            g.setColor(chromaBgColor);
        } else {
            g.setColor(bgColor);
        }

        g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

        if (!vs.isChromaScreen()) {
            super.paintComponent(g, vs);
        }
    }
    
    private CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    public void setModel(BackgroundModel model) {
        super.setModel(model);
        getCanvasDelegate().getSettingsService().storeLastBackground(model);
    }
}
