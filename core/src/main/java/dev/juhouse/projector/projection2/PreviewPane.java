/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import com.sun.javafx.geom.Rectangle;
import dev.juhouse.projector.Projector;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.util.Callback;

import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class PreviewPane extends TitledPane {
    private final ImageView imageView;

    private ByteBuffer buffer;
    private PixelBuffer<IntBuffer> pixelBuffer;

    private final CanvasDelegate delegate;

    private BridgeRender render;

    private final Callback<PixelBuffer<IntBuffer>, Rectangle2D> updateImageCallback = (buffer) -> {
        isUpdating = false;
        return null;
    };

    private boolean isUpdating;

    public PreviewPane(CanvasDelegate delegate) {
        this.delegate = delegate;

        imageView = new ImageView();

        imageView.fitHeightProperty().bind(heightProperty().subtract(20));
        imageView.fitWidthProperty().bind(widthProperty());

        imageView.setPreserveRatio(true);
        imageView.setScaleY(-1);

        setContent(imageView);
        setCollapsible(false);

        isUpdating = false;
    }

    public void setBridgeRender(BridgeRender render) {
        this.render = render;

        buffer = ByteBuffer.allocateDirect(render.getWidth() * render.getHeight() * 4);
        pixelBuffer = new PixelBuffer<>(render.getWidth(), render.getHeight(), buffer.asIntBuffer(), PixelFormat.getIntArgbPreInstance());

        imageView.setImage(new WritableImage(pixelBuffer));

        setText(render.getRenderName());
    }

    public void downloadImage() {
        if (isUpdating) {
            return;
        }

        delegate.getBridge().downloadPreviewData(render.getRenderId(), buffer);
    }

    public void updateImage() {
        isUpdating = true;
        pixelBuffer.updateBuffer(updateImageCallback);
    }

}
