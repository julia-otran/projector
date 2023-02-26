/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


/**
 *
 * @author guilherme
 */
public class PreviewImageView extends ImageView implements Runnable {
    private PixelBufferProvider bufferProvider;

    private BufferedImage targetRender;
    private Graphics2D targetGraphics;
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

            if (targetRender == null || targetRender.getWidth() != width || targetRender.getHeight() != height) {
                if (targetGraphics != null) {
                    targetGraphics.dispose();
                }

                targetRender = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                targetGraphics = targetRender.createGraphics();

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

            updateTargetRenderIfNeeded();

            int width = targetRender.getWidth();
            int height = targetRender.getHeight();

            targetGraphics.setColor(Color.BLACK);
            targetGraphics.fillRect(0, 0, width, height);

            if (bufferProvider != null) {
                int dw = bufferProvider.getWidth();
                int dh = bufferProvider.getHeight();

                double scaleX = width / (double) dw;
                double scaleY = height / (double) dh;
                double scale = Math.min(scaleX, scaleY);

                int pw = (int) Math.round(dw * scale);
                int ph = (int) Math.round(dh * scale);
                int px = (width - pw) / 2;
                int py = (height - ph) / 2;

                AffineTransform old = targetGraphics.getTransform();
                targetGraphics.translate(px, py);
                targetGraphics.scale(scale, scale);

                // TODO: Copy image from pixel buffer provider

                targetGraphics.setTransform(old);

                SwingFXUtils.toFXImage(targetRender, fxTargetRender);

                Platform.runLater(() -> {
                    setImage(fxTargetRender);
                    repainting = false;
                });
            }
        }
    }
}
