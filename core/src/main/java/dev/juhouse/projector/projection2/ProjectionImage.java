/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import dev.juhouse.projector.projection2.models.BackgroundProvide;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 *
 * @author guilherme
 */
public class ProjectionImage implements Projectable {

    protected final CanvasDelegate canvasDelegate;

    private boolean cropBackground;

    private boolean render;
    private BackgroundProvide model;

    public ProjectionImage(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
    }

    @Override
    public void init() {

    }

    @Override
    public void finish() {

    }

    @Override
    public void rebuild() {

    }

    @Override
    public void setRender(boolean render) {
        this.render = render;
        update();
    }

    public boolean getCropBackground() {
        return cropBackground;
    }

    public boolean getRender() {
        return render;
    }

    public void setCropBackground(boolean cropBackground) {
        if (this.cropBackground != cropBackground) {
            this.cropBackground = cropBackground;
            update();
        }
    }

    public BackgroundProvide getModel() {
        return model;
    }

    public void setModel(BackgroundProvide model) {
        this.model = model;
        update();
    }

    protected void update() {
        if (render) {
            BufferedImage image = getModel().getStaticBackground();

            canvasDelegate.getBridge().setImageAsset(
                    ((DataBufferInt) image.getRaster().getDataBuffer()).getData(),
                    image.getWidth(),
                    image.getHeight(),
                    cropBackground
            );
        } else {
            canvasDelegate.getBridge().setImageAsset(null, 0, 0, cropBackground);
        }
    }
}
