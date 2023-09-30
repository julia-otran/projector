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
public class ProjectionImage extends ProjectionBaseImage {

    public ProjectionImage(CanvasDelegate canvasDelegate) {
        super(canvasDelegate);
    }

    @Override
    public void init() {
        super.init();
        getRenderFlag().applyDefault(BridgeRender::getEnableRenderImage);
    }
}
