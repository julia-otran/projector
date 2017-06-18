/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 *
 * @author guilherme
 */
public class ProjectionImage implements Projectable {
    protected CanvasDelegate canvasDelegate;
    
    private BufferedImage img;
    private BufferedImage scaled;
    private boolean cropBackground;
    
    ProjectionImage(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if (scaled != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(scaled, 0, 0, null);
        }
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    @Override
    public void rebuildLayout() {
        if (img == null) {
            scaled = null;
            return;
        }

        int imgW = img.getWidth();
        int imgH = img.getHeight();

        int width = canvasDelegate.getWidth();
        int height = canvasDelegate.getHeight();

        double scaleX = width / (double) imgW;
        double scaleY = height / (double) imgH;
        double scale;
        
        if (cropBackground) {
            scale = Math.max(scaleX, scaleY);
        } else {
            scale = Math.min(scaleX, scaleY);
        }

        int newW = (int) Math.round(imgW * scale);
        int newH = (int) Math.round(imgH * scale);

        int x = (width - newW) / 2;
        int y = (height - newH) / 2;

        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.scale(scale, scale);

        BufferedImage scaling = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaling.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setTransform(at);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        
        this.scaled = scaling;
    }

    @Override
    public void init() {
        rebuildLayout();
    }

    public void setImage(BufferedImage img) {
        this.img = img;
        rebuildLayout();
    }

    public void setCropBackground(boolean cropBackground) {
        if (this.cropBackground != cropBackground) {
            this.cropBackground = cropBackground;
            rebuildLayout();
        }
    }
    
}
