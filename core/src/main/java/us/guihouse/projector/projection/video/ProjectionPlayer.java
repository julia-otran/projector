/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection.video;

import java.awt.image.BufferedImage;
import java.io.File;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import us.guihouse.projector.projection.CanvasDelegate;

/**
 *
 * @author guilherme
 */
public class ProjectionPlayer extends ProjectionVideo {
    private PlayerPreview preview;

    public ProjectionPlayer(CanvasDelegate delegate) {
        super(delegate);
    }

    @Override
    public void init() {
        if (preview == null) {
            preview = new PlayerPreview();
        }

        super.init();
        preview.start();
    }

    @Override
    public void finish() {
        preview.stop();
        super.finish();
    }

    public PlayerPreview getPreviewPanel() {
        return preview;
    }

    public void loadMedia(File file) {
        this.player.media().prepare(file.getAbsolutePath());
    }

    @Override
    protected void generateBuffer(int w, int h) {
        super.generateBuffer(w, h);
        preview.recreatePreview(w, h);
    }

    public class PlayerPreview extends ImageView implements Runnable {
        private WritableImage fxImage;
        private Thread previewThread;
        private boolean running;

        public PlayerPreview() {
            setPreserveRatio(true);
        }

        public void recreatePreview(int width, int height) {
            fxImage = new WritableImage(width, height);

            Platform.runLater(() -> setImage(fxImage));
        }

        public void start() {
            previewThread = new Thread(this);

            running = true;
            previewThread.start();
        }

        public void stop() {
            running = false;
            if (previewThread != null) {
                try {
                    previewThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                previewThread = null;
            }
        }

        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                BufferedImage src = ProjectionPlayer.this.getImage();
                WritableImage dst = fxImage;

                if (dst == null || src == null) {
                    continue;
                }

                SwingFXUtils.toFXImage(src, dst);
            }
        }
    }
}
