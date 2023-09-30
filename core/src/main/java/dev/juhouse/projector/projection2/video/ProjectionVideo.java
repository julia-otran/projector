package dev.juhouse.projector.projection2.video;

import dev.juhouse.projector.projection2.BridgeRenderFlag;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;
import dev.juhouse.projector.projection2.CanvasDelegate;
import dev.juhouse.projector.utils.VlcPlayerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class ProjectionVideo {
    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    @Getter
    @Setter
    private boolean enablePreview;

    @Getter
    private boolean cropVideo = false;

    @Getter
    private final BooleanProperty render = new SimpleBooleanProperty(false);

    private final BridgeRenderFlag renderFlag;

    public ProjectionVideo(CanvasDelegate delegate) {
        this.delegate = delegate;

        renderFlag = new BridgeRenderFlag(delegate);
        renderFlag.getFlagValueProperty().addListener(observable -> updateRender());
    }

    public BridgeRenderFlag getRenderFlag() {
        return renderFlag;
    }

    public void init() {
        this.player = VlcPlayerFactory.getFactory().mediaPlayers().newMediaPlayer();

        delegate.getBridge().attachPlayer(this.player);

        this.player.video().setAdjustVideo(false);
    }

    public void rebuild() {
        updateRender();
    }

    public void setRender(boolean render) {
        if (this.render.get() != render) {
            this.render.setValue(render);
            updateRender();
        }
    }

    private void updateRender() {
        if (render.get()) {
            this.delegate.getBridge().setVideoRenderFlag(this.player, this.cropVideo, renderFlag.getFlagValue());
        } else {
            this.delegate.getBridge().setVideoRenderFlag(this.player, this.cropVideo, BridgeRenderFlag.NO_RENDER);
        }
    }

    public void setCropVideo(boolean cropVideo) {
        this.cropVideo = cropVideo;
        updateRender();
    }

    public void finish() {
        this.player.release();
    }

    public MediaPlayer getPlayer() {
        return player;
    }
}
