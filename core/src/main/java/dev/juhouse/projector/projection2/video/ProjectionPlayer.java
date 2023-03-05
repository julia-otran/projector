/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.video;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import dev.juhouse.projector.projection2.Projectable;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionPlayer implements Projectable {
    private final PlayerPreview preview;
    private final ProjectionVideo video;

    public ProjectionPlayer(ProjectionVideo video) {
        this.video = video;
        this.preview = new PlayerPreview();

        video.setEnablePreview(true);
        video.setPreviewBufferCallback(this.preview);
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
        video.init();
    }

    @Override
    public void finish() {
        video.finish();
        video.setPreviewBufferCallback(null);
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

    public static class PlayerPreview extends ImageView implements ProjectionVideo.PreviewBufferCallback {
        private boolean running;
        private PixelBuffer<IntBuffer> previewImagePixelBuffer;

        public PlayerPreview() {
            setPreserveRatio(true);
        }

        @Override
        public void onPreviewBufferChange(ByteBuffer buffer, int w, int h) {
            Platform.runLater(() -> {
                previewImagePixelBuffer = new PixelBuffer<>(w, h, buffer.asIntBuffer(), PixelFormat.getIntArgbPreInstance());
                setImage(new WritableImage(previewImagePixelBuffer));
            });
        }

        @Override
        public void onPreviewBufferUpdated() {
            if (running) {
                return;
            }

            running = true;

            Platform.runLater(() -> {
                setImage(new WritableImage(previewImagePixelBuffer));
                running = false;
            });
        }
    }
}
