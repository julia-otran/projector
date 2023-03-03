/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.video;

import java.awt.image.BufferedImage;
import java.io.File;

import dev.juhouse.projector.projection2.Projectable;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import dev.juhouse.projector.projection2.CanvasDelegate;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionPlayer implements Projectable, ProjectionVideo.BufferSizeChangeCallback {
    private PlayerPreview preview;
    private ProjectionVideo video;

    public ProjectionPlayer(ProjectionVideo video) {
        this.video = video;
        this.preview = new PlayerPreview();
    }

    public boolean isCropVideo() {
        return video.isCropVideo();
    }

    public void setCropVideo(boolean crop) {
        video.setCropVideo(crop);
    }

    public MediaPlayer getPlayer() {
        return video.player;
    }

    public void setRender(boolean render) {
        video.setRender(render);
    }

    @Override
    public void init() {
        preview.start();
        video.init();
        video.setBufferSizeChangeCallback(this);
    }

    @Override
    public void finish() {
        video.finish();
        preview.stop();
        video.setBufferSizeChangeCallback(null);
    }

    @Override
    public void rebuild() {

    }

    public PlayerPreview getPreviewPanel() {
        return preview;
    }

    public void loadMedia(File file) {
        this.video.player.media().prepare(file.getAbsolutePath());
    }

    @Override
    public void onBufferSizeChange(int width, int height) {
        preview.recreatePreview(width, height);
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

                BufferedImage src = ProjectionPlayer.this.video.getImage();
                WritableImage dst = fxImage;

                if (dst == null || src == null) {
                    continue;
                }

                SwingFXUtils.toFXImage(src, dst);
            }
        }
    }
}
