package us.guihouse.projector.projection.video;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;
import us.guihouse.projector.projection.CanvasDelegate;
import us.guihouse.projector.projection.Projectable;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.utils.VlcPlayerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ProjectionVideo implements Projectable {
    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    protected BufferedImage image;

    protected int videoW = 0;
    protected int videoH = 0;

    private final HashMap<String, Integer> width = new HashMap<>();
    private final HashMap<String, Integer> height = new HashMap<>();

    private final HashMap<String, Integer> projectionX = new HashMap<>();
    private final HashMap<String, Integer> projectionY = new HashMap<>();

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
    public void rebuildLayout() {
        if (videoW == 0 || videoH == 0) {
            return;
        }

        width.clear();
        height.clear();
        projectionX.clear();
        projectionY.clear();

        delegate.getVirtualScreens().forEach(vs -> {
            float scaleW = vs.getWidth() / (float) videoW;
            float scaleH = vs.getHeight() / (float) videoH;

            float scale;

            if (cropVideo) {
                scale = Math.max(scaleW, scaleH);
            } else {
                scale = Math.min(scaleW, scaleH);
            }

            int scaledWidth = Math.round(scale * videoW);
            int scaledHeight = Math.round(scale * videoH);

            width.put(vs.getVirtualScreenId(), scaledWidth);
            height.put(vs.getVirtualScreenId(), scaledHeight);

            projectionX.put(vs.getVirtualScreenId(), (vs.getWidth() - scaledWidth) / 2);
            projectionY.put(vs.getVirtualScreenId(), (vs.getHeight() - scaledHeight) / 2);
        });
    }

    @Override
    public void init() {
        renderCallback = new ProjectionVideo.MyRenderCallback();
        bufferFormatCallback = new ProjectionVideo.MyBufferFormatCallback();

        this.player = VlcPlayerFactory.getFactory().mediaPlayers().newMediaPlayer();
        videoSurface = VlcPlayerFactory.getFactory().videoSurfaces().newVideoSurface(bufferFormatCallback, renderCallback, true);
        this.videoSurface.attach(this.player);
        this.player.video().setAdjustVideo(false);

        rebuildLayout();
    }

    public void setCropVideo(boolean cropVideo) {
        this.cropVideo = cropVideo;
        rebuildLayout();
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        int rWidth = width.getOrDefault(vs.getVirtualScreenId(), 0);
        int rHeight = height.getOrDefault(vs.getVirtualScreenId(), 0);
        int rProjectionX = projectionX.getOrDefault(vs.getVirtualScreenId(), 0);
        int rProjectionY = projectionY.getOrDefault(vs.getVirtualScreenId(), 0);

        if (render.get() && rWidth > 0 && rHeight > 0) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, vs.getWidth(), vs.getHeight());
            g.drawImage(image, rProjectionX, rProjectionY, rWidth, rHeight, null);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    protected void generateBuffer(int w, int h) {
        videoW = w;
        videoH = h;

        image = delegate.getDefaultDevice().getDefaultConfiguration().createCompatibleImage(w, h);

        rebuildLayout();
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
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            assert buffers[0].capacity() == videoW * videoH * 4;
        }
    }
}
