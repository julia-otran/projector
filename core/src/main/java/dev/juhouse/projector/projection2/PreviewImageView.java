/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import javafx.application.Platform;
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
public class PreviewImageView extends ImageView implements Runnable {
    private ByteBuffer buffer;
    private PixelBuffer<IntBuffer> pixelBuffer;

    private final CanvasDelegate delegate;

    private boolean repainting = false;
    private boolean running = false;
    private Thread updateThread = null;

    public PreviewImageView(CanvasDelegate delegate) {
        setPreserveRatio(true);
        setScaleY(-1);
        this.delegate = delegate;
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                running = false;
                e.printStackTrace();
            }

            if (repainting) {
                continue;
            }

            repainting = true;

            delegate.getBridge().downloadPreviewData(buffer);

            Platform.runLater(() -> {
                setImage(new WritableImage(pixelBuffer));
                repainting = false;
            });
        }
    }

    public void stop() {
        if (running) {
            running = false;

            try {
                updateThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        if (!running) {
            buffer = ByteBuffer.allocateDirect(delegate.getMainWidth() * delegate.getMainHeight() * 4);
            pixelBuffer = new PixelBuffer<>(delegate.getMainWidth(), delegate.getMainHeight(), buffer.asIntBuffer(), PixelFormat.getIntArgbPreInstance());

            running = true;
            updateThread = new Thread(this);
            updateThread.start();
        }
    }
}
