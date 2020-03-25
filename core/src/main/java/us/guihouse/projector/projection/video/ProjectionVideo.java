package us.guihouse.projector.projection.video;

import lombok.Getter;
import lombok.Setter;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;
import us.guihouse.projector.projection.CanvasDelegate;
import us.guihouse.projector.projection.Projectable;
import us.guihouse.projector.utils.VlcPlayerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

public class ProjectionVideo implements Projectable {
    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    protected BufferedImage image;
    private BufferFormat bufferFormat;

    protected int videoW;
    protected int videoH;

    protected int deviceW;
    protected int deviceH;

    protected int width;
    protected int height;

    private int projectionX;
    private int projectionY;

    private final boolean cropVideo;

    @Getter
    @Setter
    private boolean render = true;

    protected GraphicsDevice device;
    protected ProjectionVideo.MyRenderCallback renderCallback;
    protected ProjectionVideo.MyBufferFormatCallback bufferFormatCallback;
    protected CallbackVideoSurface videoSurface;

    public ProjectionVideo(CanvasDelegate delegate, boolean cropVideo) {
        this.delegate = delegate;
        this.cropVideo = cropVideo;
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return delegate;
    }

    @Override
    public void rebuildLayout() {
        if (this.player != null) {
            this.player.release();
        }

        device = delegate.getDefaultDevice();
        this.deviceW = delegate.getWidth();
        this.deviceH = delegate.getHeight();
        this.image = null;

        renderCallback = new ProjectionVideo.MyRenderCallback();
        bufferFormatCallback = new ProjectionVideo.MyBufferFormatCallback();

        this.player = VlcPlayerFactory.getFactory().mediaPlayers().newMediaPlayer();
        videoSurface = VlcPlayerFactory.getFactory().videoSurfaces().newVideoSurface(bufferFormatCallback, renderCallback, true);
        this.videoSurface.attach(this.player);

        this.player.video().setAdjustVideo(false);
    }

    @Override
    public void init() {
        rebuildLayout();
    }

    @Override
    public void paintComponent(Graphics2D g) {
        if (render) {
            g.drawImage(image, projectionX, projectionY, width, height, null);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    protected void generateBuffer(int w, int h) {
        videoW = w;
        videoH = h;

        float scaleW = deviceW / (float) w;
        float scaleH = deviceH / (float) h;

        float scale;

        if (cropVideo) {
            scale = Math.max(scaleW, scaleH);
        } else {
            scale = Math.min(scaleW, scaleH);
        }

        this.width = Math.round(scale * w);
        this.height = Math.round(scale * h);

        this.projectionX = (deviceW - width) / 2;
        this.projectionY = (deviceH - height) / 2;

        image = device.getDefaultConfiguration().createCompatibleImage(w, h);

        bufferFormat = new RV32BufferFormat(w, h);
    }

    @Override
    public void finish() {
        this.player.release();
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    private final class MyRenderCallback implements RenderCallback {
        MyRenderCallback() {
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            nativeBuffers[0].asIntBuffer().get(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
        }
    }

    private final class MyBufferFormatCallback implements BufferFormatCallback {

        MyBufferFormatCallback() {
        }

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            generateBuffer(sourceWidth, sourceHeight);
            return bufferFormat;
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            assert buffers[0].capacity() == videoW * videoH * 4;
        }
    }
}
