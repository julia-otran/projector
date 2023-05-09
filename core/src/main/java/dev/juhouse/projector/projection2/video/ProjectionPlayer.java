/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.video;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import dev.juhouse.projector.projection2.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionPlayer implements Projectable {
    private final PlayerPreview preview;
    private final ProjectionVideo video;

    public ProjectionPlayer(ProjectionVideo video, CanvasDelegate delegate) {
        this.video = video;

        this.preview = new PlayerPreview(video, delegate);

        video.setEnablePreview(true);
        video.getRenderFlagProperty().get().applyDefault(BridgeRender::getEnableRenderVideo);
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
    public ReadOnlyObjectProperty<BridgeRenderFlag> getRenderFlagProperty() {
        return video.getRenderFlagProperty();
    }

    @Override
    public void init() {
        video.init();
    }

    @Override
    public void finish() {
        video.finish();
    }

    @Override
    public void rebuild() {
        video.rebuild();
    }

    public PlayerPreview getPreviewPanel() {
        return preview;
    }

    public void loadMedia(File file) {
        this.video.player.media().prepare(file.getAbsolutePath());
    }

    public static class PlayerPreview extends AnchorPane implements Runnable {
        private boolean running;
        private PixelBuffer<IntBuffer> previewImagePixelBuffer;
        private final ByteBuffer previewImageBuffer;
        private final IntBuffer previewImageBufferInt;
        private final ProjectionVideo video;
        private final CanvasDelegate delegate;
        private final ImageView previewImageView;
        private final Label previewErrorLabel;

        private boolean updating;

        public PlayerPreview(ProjectionVideo video, CanvasDelegate delegate) {
            this.previewImageView = new ImageView();
            this.previewImageView.setPreserveRatio(true);

            this.widthProperty().addListener((prop, old, newVal) -> this.previewImageView.fitWidthProperty().set(newVal.doubleValue()));
            this.heightProperty().addListener((prop, old, newVal) -> this.previewImageView.fitHeightProperty().set(newVal.doubleValue()));

            this.previewErrorLabel = new Label();
            previewErrorLabel.setTextFill(Color.color(1.0, 1.0, 1.0));
            previewErrorLabel.setPadding(new Insets(10.0));

            this.video = video;
            this.delegate = delegate;

            previewImageBuffer = ByteBuffer.allocateDirect(1920 * 1920 * 4);
            previewImageBufferInt = previewImageBuffer.asIntBuffer();
        }

        public void startPreview() {
            running = true;
            new Thread(this).start();
        }

        public void stopPreview() {
            running = false;
        }

        private void showError(String message) {
            Platform.runLater(() -> {
                previewErrorLabel.setText(message);
                getChildren().clear();
                getChildren().add(previewErrorLabel);
                updating = false;
            });
        }

        private void updatePreview() {
            if (updating) {
                return;
            }

            if (video.getRender().get()) {
                showError("[Preview Indisponível] Projetando Vídeo....");
                return;
            }

            updating = true;

            try {
                previewImageBuffer.flip();
                BridgeVideoPreviewSize outputSize = delegate.getBridge().downloadPlayerPreview(video.player, previewImageBuffer);

                if (outputSize.getWidth() == 0) {
                    updating = false;
                    return;
                }

                Platform.runLater(() -> {
                    if (!running) {
                        return;
                    }

                    previewImagePixelBuffer = new PixelBuffer<>(
                            outputSize.getWidth(),
                            outputSize.getHeight(),
                            previewImageBufferInt,
                            PixelFormat.getIntArgbPreInstance()
                    );

                    this.previewImageView.setImage(new WritableImage(previewImagePixelBuffer));

                    if (getChildren().isEmpty() || getChildren().get(0) != previewImageView) {
                        this.getChildren().clear();
                        this.getChildren().add(this.previewImageView);
                    }

                    updating = false;
                });

            } catch (Bridge.VideoPreviewOutputBufferTooSmall e) {
                showError("[Preview Indisponível] Resolução do video muito alta (max 1920x1080)....");
            }
        }

        @Override
        public void run() {
            updating = false;

            while (running) {
                updatePreview();

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        }
    }
}
