/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.image;

import dev.juhouse.projector.projection2.*;
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
    private final BridgeRenderFlag renderFlag;
    private boolean cropBackground;

    private boolean render;
    private BackgroundProvide model;

    private PresentMultipleImage presentImage;

    public ProjectionImage(CanvasDelegate canvasDelegate) {
        this.renderFlag = new BridgeRenderFlag(canvasDelegate);
        this.canvasDelegate = canvasDelegate;
        this.render = false;
    }

    @Override
    public void init() {
        renderFlag.applyDefault(BridgeRender::getEnableRenderImage);
        presentImage = new PresentMultipleImage(renderFlag, canvasDelegate.getBridge());
    }

    @Override
    public void finish() {

    }

    @Override
    public void rebuild() {
        presentImage.rebuild();
    }

    @Override
    public void setRender(boolean render) {
        if (this.render != render) {
            this.render = render;
            update();
        }
    }

    @Override
    public BridgeRenderFlag getRenderFlag() {
        return renderFlag;
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

            if (presentImage != null) {
                presentImage.setCrop(cropBackground);
            }
        }
    }

    public BackgroundProvide getModel() {
        return model;
    }

    public void setModel(BackgroundProvide model) {
        this.model = model;
        updateModel();
        update();
    }

    private void updateModel() {
        BackgroundProvide model = getModel();

        if (model == null) {
            presentImage.update(null, 0, 0, cropBackground);
            return;
        }

        Image image = model.getStaticBackground();

        if (image == null) {
            presentImage.update(null, 0, 0, cropBackground);
            return;
        }

        int w = (int) Math.round(image.getWidth());
        int h = (int) Math.round(image.getHeight());

        IntBuffer buffer = IntBuffer.allocate(w * h);
        image.getPixelReader().getPixels(0, 0, w, h, WritablePixelFormat.getIntArgbInstance(), buffer.array(), 0, w);

        presentImage.update(buffer.array(), w, h, cropBackground);
    }

    private void update() {
        presentImage.setRender(render);
    }
}
