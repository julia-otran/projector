/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import us.guihouse.projector.projection.models.VirtualScreen;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


/**
 *
 * @author guilherme
 */
public class PreviewImageView extends ImageView implements Runnable {
    private final CanvasDelegate delegate;
    private ProjectionCanvas projectionCanvas;

    private BufferedImage targetRender;
    private Graphics2D targetGraphics;
    private WritableImage fxTargetRender;

    private boolean repainting = false;
    private boolean running = false;
    private Thread updateThread = null;

    public PreviewImageView(CanvasDelegate delegate) {
        this.delegate = delegate;
        setPreserveRatio(true);
    }

    private void updateTargetRenderIfNeeded() {
        int width = delegate.getMainWidth();
        int height = delegate.getMainHeight();

        if (targetRender == null || targetRender.getWidth() != width || targetRender.getHeight() != height) {
            if (targetGraphics != null) {
                targetGraphics.dispose();
            }

            targetRender = delegate.getDefaultDevice().getDefaultConfiguration().createCompatibleImage(width, height);
            targetGraphics = targetRender.createGraphics();

            fxTargetRender = new WritableImage(width, height);
        }
    }

    void setProjectionCanvas(ProjectionCanvas projectionCanvas) {
        this.projectionCanvas = projectionCanvas;

        if (projectionCanvas == null) {
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

            VirtualScreen main = delegate.getVirtualScreens().stream().filter(VirtualScreen::isMainScreen).findFirst().orElse(null);

            if (projectionCanvas != null && main != null) {
                int dw = delegate.getMainWidth();
                int dh = delegate.getMainHeight();

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
                projectionCanvas.paintComponent(targetGraphics, main);
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
