/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class PreviewPane extends TitledPane {
    private ImageView imageView;

    private ByteBuffer buffer;
    private PixelBuffer<IntBuffer> pixelBuffer;

    private final CanvasDelegate delegate;

    private BridgeRender render;

    public PreviewPane(CanvasDelegate delegate) {
        this.delegate = delegate;

        imageView = new ImageView();

        imageView.fitHeightProperty().bind(heightProperty().subtract(20));
        imageView.fitWidthProperty().bind(widthProperty());

        imageView.setPreserveRatio(true);
        imageView.setScaleY(-1);

        setContent(imageView);
        setCollapsible(false);
    }

    public void setBridgeRender(BridgeRender render) {
        this.render = render;

        buffer = ByteBuffer.allocateDirect(render.getWidth() * render.getHeight() * 4);
        pixelBuffer = new PixelBuffer<>(render.getWidth(), render.getHeight(), buffer.asIntBuffer(), PixelFormat.getIntArgbPreInstance());

        setText(render.getRenderName());
    }

    public void downloadImage() {
        delegate.getBridge().downloadPreviewData(render.getRenderId(), buffer);
    }

    public void updateImage() {
        imageView.setImage(new WritableImage(pixelBuffer));
    }

}
