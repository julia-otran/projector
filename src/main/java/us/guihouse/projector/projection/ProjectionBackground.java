/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author guilherme
 */
public class ProjectionBackground extends ProjectionImage {


    ProjectionBackground(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);
    }

    @Override
    public void paintComponent(Graphics g) {
        // Prevent monitor to sleep!
        g.setColor(new Color(10, 10, 10));
        g.fillRect(0, 0, canvasDelegate.getWidth(), canvasDelegate.getHeight());
        
        super.paintComponent(g);
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }
}
