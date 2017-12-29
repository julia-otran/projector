/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import us.guihouse.projector.projection.models.BackgroundModel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author guilherme
 */
public class ProjectionBackground extends ProjectionImage {
    private final Color bgColor;
    
    ProjectionBackground(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);
        
        // Place some color to prevent monitor from sleeping
        bgColor = new Color(20, 20, 20);

        this.setModel(getCanvasDelegate().getSettingsService().getLastBackground());
    }

    @Override
    public void paintComponent(Graphics2D g) {
        if (!hasImage()) {
            g.setColor(bgColor);
            g.fillRect(0, 0, canvasDelegate.getWidth(), canvasDelegate.getHeight());
        }
        
        super.paintComponent(g);
    }
    
    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    @Override
    public void init() {
        super.init();
    }

    public void setModel(BackgroundModel model) {
        super.setModel(model);
        getCanvasDelegate().getSettingsService().storeLastBackground(model);
    }
}
