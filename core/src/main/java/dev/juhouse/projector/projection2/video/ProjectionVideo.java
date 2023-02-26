package dev.juhouse.projector.projection2.video;

import javafx.beans.property.BooleanProperty;
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
import dev.juhouse.projector.projection2.Projectable;
import dev.juhouse.projector.utils.VlcPlayerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ProjectionVideo implements Projectable {
    public interface BufferSizeChangeCallback {
        void onBufferSizeChange(int width, int height);
    }
    @Getter
    @Setter
    private BufferSizeChangeCallback bufferSizeChangeCallback;

    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    protected BufferedImage image;

    protected int videoW = 0;
    protected int videoH = 0;

    @Getter
    private boolean cropVideo = false;

    @Getter
    private final BooleanProperty render = new SimpleBooleanProperty(true);

    protected ProjectionVideo.MyRenderCallback renderCallback;
    protected ProjectionVideo.MyBufferFormatCallback bufferFormatCallback;
    protected CallbackVideoSurface videoSurface;

    public ProjectionVideo(CanvasDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void init() {
        renderCallback = new ProjectionVideo.MyRenderCallback();
        bufferFormatCallback = new ProjectionVideo.MyBufferFormatCallback();

        this.player = VlcPlayerFactory.getFactory().mediaPlayers().newMediaPlayer();
        videoSurface = VlcPlayerFactory.getFactory().videoSurfaces().newVideoSurface(bufferFormatCallback, renderCallback, true);
        this.videoSurface.attach(this.player);
        this.player.video().setAdjustVideo(false);
    }

    public void setRender(boolean render) {
        this.render.setValue(render);
        delegate.getBridge().setRenderVideoBuffer(render);
    }

    public void setCropVideo(boolean cropVideo) {
        this.cropVideo = cropVideo;
    }

    public BufferedImage getImage() {
        return image;
    }

    protected void generateBuffer(int w, int h) {
        videoW = w;
        videoH = h;

        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        if (bufferSizeChangeCallback != null) {
            bufferSizeChangeCallback.onBufferSizeChange(w, h);
        }
    }

    @Override
    public void finish() {
        this.player.release();
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    private final class MyRenderCallback implements RenderCallback {
        private int previewUpdateCount;

        MyRenderCallback() {
            previewUpdateCount = 0;
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            if (previewUpdateCount >= 10) {
                nativeBuffers[0].asIntBuffer().get(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
                previewUpdateCount = 0;
            }

            if (render.get()) {
                delegate.getBridge().setVideoBuffer(nativeBuffers[0], videoW, videoH, cropVideo);
            }
        }
    }

    private final class MyBufferFormatCallback implements BufferFormatCallback {
        private final HashMap<String, RV32BufferFormat> buffers = new HashMap<>();

        MyBufferFormatCallback() {
        }

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            String bufferIdentifier = sourceWidth + "x" + sourceHeight;
            RV32BufferFormat buffer = buffers.get(bufferIdentifier);

            if (buffer == null) {
                buffer = new RV32BufferFormat(sourceWidth, sourceHeight);
                buffers.put(bufferIdentifier, buffer);
            }

            generateBuffer(sourceWidth, sourceHeight);

            return buffer;
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            assert buffers[0].capacity() == videoW * videoH * 4;
        }
    }
}
