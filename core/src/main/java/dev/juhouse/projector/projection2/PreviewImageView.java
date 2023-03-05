/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class PreviewImageView extends ImageView implements Runnable {
    private PixelBufferProvider bufferProvider;
    private WritableImage fxTargetRender;

    private boolean repainting = false;
    private boolean running = false;
    private Thread updateThread = null;

    public PreviewImageView() {
        setPreserveRatio(true);
    }

    private void updateTargetRenderIfNeeded() {
        if (bufferProvider != null) {
            int width = bufferProvider.getWidth();
            int height = bufferProvider.getHeight();

            if (fxTargetRender == null || Math.round(fxTargetRender.getWidth()) != width || Math.round(fxTargetRender.getHeight()) != height) {
                fxTargetRender = new WritableImage(width, height);
            }
        }
    }

    void setPixelBufferProvider(PixelBufferProvider provider) {
        this.bufferProvider = provider;

        if (provider == null) {
            running = false;
            if (updateThread != null) {
                try {
                    updateThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateThread = null;
            }
        } else {
            running = true;
            repainting = false;
            updateThread = new Thread(this);
            updateThread.start();
        }
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

            if (bufferProvider != null) {
                updateTargetRenderIfNeeded();
                // TODO: Copy image from pixel buffer provider

                Platform.runLater(() -> {
                    setImage(fxTargetRender);
                    repainting = false;
                });
            }
        }
    }
}
