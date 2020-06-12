/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import us.guihouse.projector.projection.models.BackgroundModel;
import us.guihouse.projector.projection.models.BackgroundProvide;
import us.guihouse.projector.projection.models.VirtualScreen;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 *
 * @author guilherme
 */
public class ProjectionImage implements Projectable {

    protected final CanvasDelegate canvasDelegate;

    private final Color bgColor;

    private final HashMap<String, BufferedImage> scaledBackground = new HashMap<>();

    private boolean cropBackground;
    private BackgroundProvide model;

    ProjectionImage(CanvasDelegate canvasDelegate) {
        this(canvasDelegate, new Color(0, 0, 0));
    }

    public ProjectionImage(CanvasDelegate canvasDelegate, Color bgColor) {
        this.canvasDelegate = canvasDelegate;
        this.bgColor = bgColor;
    }

    public boolean isEmpty() {
        return scaledBackground.isEmpty();
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        if (isEmpty()) {
            return;
        }

        g.setColor(bgColor);
        g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

        BufferedImage render = scaledBackground.get(vs.getVirtualScreenId());

        if (render != null) {
            g.drawImage(render, 0, 0, null);
        }
    }

    @Override
    public void rebuildLayout() {
        scaledBackground.clear();

        if (model == null) {
            return;
        }

        if (model.getStaticBackground() != null) {
            scaleBackground(model.getStaticBackground());
        }
    }

    private void scaleBackground(BufferedImage img) {
        scaledBackground.clear();

        canvasDelegate.getVirtualScreens()
            .forEach(vs -> {
                double imgW = img.getWidth();
                double imgH = img.getHeight();

                int width = vs.getWidth();
                int height = vs.getHeight();

                if (width == 0 || height == 0) {
                    return;
                }

                double scaleX = width / imgW;
                double scaleY = height / imgH;
                double backgroundScale;

                if (cropBackground) {
                    backgroundScale = Math.max(scaleX, scaleY);
                } else {
                    backgroundScale = Math.min(scaleX, scaleY);
                }

                int newW = (int) Math.round(imgW * backgroundScale);
                int newH = (int) Math.round(imgH * backgroundScale);

                int x = (width - newW) / 2;
                int y = (height - newH) / 2;

                AffineTransform at = new AffineTransform();
                at.translate(x, y);
                at.scale(backgroundScale, backgroundScale);

                BufferedImage scaling = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = scaling.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setTransform(at);
                g2.drawImage(img, 0, 0, null);
                g2.dispose();

                this.scaledBackground.put(vs.getVirtualScreenId(), scaling);
            });
    }

    @Override
    public void init() {
        rebuildLayout();
    }

    @Override
    public void finish() {

    }

    public boolean getCropBackground() {
        return cropBackground;
    }

    public void setCropBackground(boolean cropBackground) {
        if (this.cropBackground != cropBackground) {
            this.cropBackground = cropBackground;
            rebuildLayout();
        }
    }

    public BackgroundProvide getModel() {
        return model;
    }

    public void setModel(BackgroundProvide model) {
        this.model = model;
        rebuildLayout();
    }
}
