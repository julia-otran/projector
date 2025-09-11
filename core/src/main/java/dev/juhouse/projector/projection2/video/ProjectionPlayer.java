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
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
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

        this.preview = new PlayerPreview(new PlayerPreviewCallback() {
            @Override
            public boolean isRender() {
                return video.getRender().get();
            }

            @NotNull
            @Override
            public PlayerPreviewCallbackFrameSize getFrame(@NotNull ByteBuffer buffer) throws Bridge.VideoPreviewOutputBufferTooSmall {
                BridgeVideoPreviewSize previewSize = delegate.getBridge().downloadPlayerPreview(getPlayer(), buffer);
                return new PlayerPreviewCallbackFrameSize(previewSize.getWidth(), previewSize.getHeight(), PlayerPreviewCallbackFramePixelFormat.GL_RGBA);
            }
        }, delegate);

        video.setEnablePreview(true);
    }

    public void applyRenderFlag() {
        video.getRenderFlag().applyDefault(BridgeRender::getEnableRenderVideo);
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
    public BridgeRenderFlag getRenderFlag() {
        return video.getRenderFlag();
    }

    @Override
    public void init() {
        applyRenderFlag();
        video.init();
        video.player.audio().setMute(true);
    }

    @Override
    public void finish() {
        video.finish();
    }

    @Override
    public void rebuild() {
        video.rebuild();
        applyRenderFlag();
    }

    public PlayerPreview getPreviewPanel() {
        return preview;
    }

    public void loadMedia(File file) {
        this.video.player.media().prepare(file.getAbsolutePath());
    }
}
