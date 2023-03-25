/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.image;

import dev.juhouse.projector.projection2.BridgeRender;
import dev.juhouse.projector.projection2.BridgeRenderFlag;
import dev.juhouse.projector.projection2.CanvasDelegate;
import dev.juhouse.projector.projection2.Projectable;
import dev.juhouse.projector.projection2.models.BackgroundProvide;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.image.Image;
import javafx.scene.image.WritablePixelFormat;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionImage implements Projectable {

    protected final CanvasDelegate canvasDelegate;
    private final ReadOnlyObjectWrapper<BridgeRenderFlag> renderFlag;
    private boolean cropBackground;

    private boolean render;
    private BackgroundProvide model;

    public ProjectionImage(CanvasDelegate canvasDelegate) {
        this.renderFlag = new ReadOnlyObjectWrapper<>(new BridgeRenderFlag(canvasDelegate));
        this.canvasDelegate = canvasDelegate;
    }

    @Override
    public void init() {
        renderFlag.get().getFlagValueProperty().addListener((observableValue, number, t1) -> update());
        renderFlag.get().applyDefault(BridgeRender::getEnableRenderImage);
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

    @Override
    public ReadOnlyObjectProperty<BridgeRenderFlag> getRenderFlagProperty() {
        return renderFlag.getReadOnlyProperty();
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
        if (buffer != null) {
            canvasDelegate.getBridge().setImageAsset(buffer, width, height, crop, renderFlag.get().getFlagValue());
        } else {
            canvasDelegate.getBridge().setImageAsset(null, width, height, crop, BridgeRenderFlag.NO_RENDER);
        }
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
