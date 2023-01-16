package dev.juhouse.projector.projection.video;

import dev.juhouse.projector.projection.models.VirtualScreen;
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
import dev.juhouse.projector.projection.CanvasDelegate;
import dev.juhouse.projector.projection.Paintable;
import dev.juhouse.projector.projection.PaintableCrossFader;
import dev.juhouse.projector.projection.Projectable;
import dev.juhouse.projector.utils.VlcPlayerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ProjectionVideo implements Projectable {
    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    protected BufferedImage image;
    protected BufferedImage freeze = null;
    protected final HashMap<String, BufferedImage> imageCache = new HashMap<>();
    protected final HashMap<String, BufferedImage> freezeCache = new HashMap<>();

    protected boolean firstFrame = false;
    protected boolean isFreeze = false;

    @Getter
    @Setter
    protected boolean shouldFadeIn = false;
    @Getter
    @Setter
    protected boolean useFade = false;

    protected final HashMap<String, PaintableCrossFader> faders = new HashMap<>();
    protected final Paintable fadePaintable = new VideoPaintable();

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

        delegate.getVirtualScreens().forEach(vs -> {
            if (!faders.containsKey(vs.getVirtualScreenId())) {
                faders.put(vs.getVirtualScreenId(), new PaintableCrossFader(vs, true));
            }

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
        if (render.get()) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

            if (useFade) {
                PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

                if (fader != null) {
                    fader.paintComponent(g);
                }
            } else {
                fadePaintable.paintComponent(g, vs);
            }
        }
    }

    private class VideoPaintable implements Paintable {
        @Override
        public void paintComponent(Graphics2D g, VirtualScreen vs) {
            int rWidth = width.getOrDefault(vs.getVirtualScreenId(), 0);
            int rHeight = height.getOrDefault(vs.getVirtualScreenId(), 0);
            int rProjectionX = projectionX.getOrDefault(vs.getVirtualScreenId(), 0);
            int rProjectionY = projectionY.getOrDefault(vs.getVirtualScreenId(), 0);

            if (rWidth > 0 && rHeight > 0) {
                if (isFreeze) {
                    g.drawImage(freeze, rProjectionX, rProjectionY, rWidth, rHeight, null);
                } else {
                    g.drawImage(image, rProjectionX, rProjectionY, rWidth, rHeight, null);
                }
            }
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public void freeze() {
        if (!isFreeze) {
            firstFrame = true;

            String freezeSizeIdentifier = videoW + "x" + videoH;
            freeze = freezeCache.get(freezeSizeIdentifier);

            if (image != null && freeze != null) {
                image.copyData(freeze.getRaster());
            }
        }
        isFreeze = true;
    }

    public void unfreeze() {
        if (isFreeze) {
            rebuildLayout();
        }
        isFreeze = false;
    }

    protected void generateBuffer(int w, int h) {
        freeze();

        videoW = w;
        videoH = h;

        String sizeIdentifier = w + "x" + h;
        image = imageCache.get(sizeIdentifier);

        if (image == null) {
            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            imageCache.put(sizeIdentifier, image);
        }

        if (!freezeCache.containsKey(sizeIdentifier)) {
            freezeCache.put(sizeIdentifier, new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB));
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
        MyRenderCallback() {
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            if (firstFrame) {
                firstFrame = false;
            } else {
                nativeBuffers[0].asIntBuffer().get(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
                unfreeze();

                if (shouldFadeIn) {
                    faders.values().forEach(f -> f.fadeIn(fadePaintable));
                    shouldFadeIn = false;
                }
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
