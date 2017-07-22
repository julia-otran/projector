/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

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
        this.setImage(getCanvasDelegate().getSettingsService().getLastBackgroundImageFile());
    }

    void setImage(File selectedFile) {
        if (selectedFile == null) {
            super.setImage(null);
            getCanvasDelegate().getSettingsService().storeLastBackground(null);
            return;
        }

        try {
            BufferedImage image = ImageIO.read(selectedFile);
            getCanvasDelegate().getSettingsService().storeLastBackground(selectedFile);
            super.setImage(image);
        } catch (IOException ex) {
            Logger.getLogger(ProjectionCanvas.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
