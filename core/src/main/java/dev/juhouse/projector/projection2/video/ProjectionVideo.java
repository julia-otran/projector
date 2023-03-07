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
import dev.juhouse.projector.utils.VlcPlayerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class ProjectionVideo {
    public interface PreviewBufferCallback {
        void onPreviewBufferChange(ByteBuffer buffer, int width, int height);
        void onPreviewBufferUpdated();
    }

    @Getter
    @Setter
    private PreviewBufferCallback previewBufferCallback;

    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    @Getter
    @Setter
    private boolean enablePreview;

    protected int videoW = 0;
    protected int videoH = 0;

    @Getter
    private boolean cropVideo = false;

    @Getter
    private final BooleanProperty render = new SimpleBooleanProperty(false);

    protected ProjectionVideo.MyRenderCallback renderCallback;
    protected ProjectionVideo.MyBufferFormatCallback bufferFormatCallback;
    protected CallbackVideoSurface videoSurface;

    private ByteBuffer[] buffers;

    public ProjectionVideo(CanvasDelegate delegate) {
        this.delegate = delegate;
    }
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
        this.delegate.getBridge().setRenderVideoBuffer(render);
        updateBufferAddress();
    }

    public void setCropVideo(boolean cropVideo) {
        this.cropVideo = cropVideo;
        updateBufferAddress();
    }

    protected void setBufferSize(int w, int h) {
        if (videoW != w || videoH != h) {
            videoW = w;
            videoH = h;

            if (enablePreview && previewBufferCallback != null) {
                previewBufferCallback.onPreviewBufferChange(buffers[0], w, h);
            }
        }
    }

    public void finish() {
        this.player.release();
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    private final class MyRenderCallback implements RenderCallback {
        private long previewUpdateTime;

        MyRenderCallback() {
            previewUpdateTime = 0;
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            if (enablePreview) {
                long ms = System.currentTimeMillis();

                if (ms - previewUpdateTime > 100) {
                    if (previewBufferCallback != null) {
                        previewBufferCallback.onPreviewBufferUpdated();
                    }

                    previewUpdateTime = ms;
                }
            }

            if (render.get()) {
                delegate.getBridge().updateVideoBuffer();
            }
        }
    }

    private void updateBufferAddress() {
        if (buffers != null && buffers[0] != null && render.get()) {
            delegate.getBridge().setVideoBuffer(buffers[0], videoW, videoH, cropVideo);
        }
    }

    private final class MyBufferFormatCallback implements BufferFormatCallback {
        private final HashMap<String, RV32BufferFormat> buffers = new HashMap<>();

        int sourceWidth;
        int sourceHeight;

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

            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;

            return buffer;
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            assert buffers[0].capacity() == sourceWidth * sourceHeight * 4;

            ProjectionVideo.this.buffers = buffers;
            setBufferSize(sourceWidth, sourceHeight);
            updateBufferAddress();
        }
    }
}
