/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import dev.juhouse.projector.projection2.models.BackgroundProvide;
import javafx.scene.image.Image;
import javafx.scene.image.WritablePixelFormat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
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

    protected void setImageAsset(int[] buffer, int width, int height, boolean crop) {
        canvasDelegate.getBridge().setImageAsset(buffer, width, height, crop);
    }

    private void update() {
        if (render) {
            Image image = getModel().getStaticBackground();

            if (image == null) {
                setImageAsset(null, 0, 0, cropBackground);
            } else {
                int w = (int) Math.round(image.getWidth());
                int h = (int) Math.round(image.getHeight());

                IntBuffer buffer = IntBuffer.allocate(w * h);
                image.getPixelReader().getPixels(0, 0, w, h, WritablePixelFormat.getIntArgbInstance(), buffer.array(), 0, w);

                setImageAsset(buffer.array(), w, h, cropBackground);
            }
        } else {
            setImageAsset(null, 0, 0, cropBackground);
        }
    }
}
