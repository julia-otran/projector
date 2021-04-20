package us.guihouse.projector.projection.video;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.w3c.dom.css.Rect;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;
import us.guihouse.projector.projection.CanvasDelegate;
import us.guihouse.projector.projection.PaintableCrossFader;
import us.guihouse.projector.projection.Projectable;
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapter;
import us.guihouse.projector.projection.glfw.RGBImageCopy;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.utils.VlcPlayerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProjectionVideo implements Projectable {
    private final CanvasDelegate delegate;

    protected MediaPlayer player;

    private int[] imageData;
    protected int videoW = 0;
    protected int videoH = 0;

    protected boolean firstFrame = false;
    private final ConcurrentHashMap<String, Boolean> freezes = new ConcurrentHashMap<>();
    private boolean freeze = false;

    private int[] freezeImageData;

    private final ConcurrentHashMap<String, PaintableCrossFader> faders = new ConcurrentHashMap<>();

    @Getter
    private boolean cropVideo = false;

    @Getter
    private final BooleanProperty render = new SimpleBooleanProperty(true);

    protected ProjectionVideo.MyRenderCallback renderCallback;
    protected ProjectionVideo.MyBufferFormatCallback bufferFormatCallback;
    protected CallbackVideoSurface videoSurface;

    private final Queue<int[]> frameBuffer = new ConcurrentLinkedQueue<>();

    private ProjectionVideoPaintable videoPaintable;

    @Getter
    @Setter
    private boolean fadeInVideo = false;

    private boolean fadePending = true;

    private boolean finished;

    public ProjectionVideo(CanvasDelegate delegate) {
        this.delegate = delegate;

        render.addListener((prop, oldValue, newValue) -> {
            if (!oldValue && newValue) {
                fadePending = true;
            }
        });
    }

    @Override
    public void rebuildLayout() {
        rebuildLayout(true);
    }

    public void rebuildLayout(boolean createFaders) {
        delegate.getVirtualScreens().forEach(vs -> {
            if (createFaders) {
                PaintableCrossFader fader = new PaintableCrossFader(vs);
                fader.setCascadeFade(true);
                faders.put(vs.getVirtualScreenId(), fader);
            }
        });

        if (createFaders) {
            fadePending = true;
        }

        if (videoW == 0 || videoH == 0) {
            return;
        }

        Rectangle videoSize = new Rectangle(videoW, videoH);

        delegate.getVirtualScreens().forEach(vs -> {
            freezes.put(vs.getVirtualScreenId(), true);

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
            int x = (vs.getWidth() - scaledWidth) / 2;
            int y = (vs.getHeight() - scaledHeight) / 2;

            Rectangle position = new Rectangle(x, y, scaledWidth, scaledHeight);

            delegate.runOnProvider(vs, provider -> {
                videoPaintable.generateTex(provider, vs, position, videoSize);
                freezes.put(vs.getVirtualScreenId(), false);
            });
        });
    }

    @Override
    public void init() {
        finished = false;
        videoPaintable = new ProjectionVideoPaintable();
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

    private void updateImages() {
        delegate.getVirtualScreens().forEach(vs -> {
            if (freezes.getOrDefault(vs.getVirtualScreenId(), false)) {
                videoPaintable.setProjectionData(freezeImageData, vs);
            } else {
                videoPaintable.setProjectionData(imageData, vs);
            }
        });
    }

    private void fadeInVideo() {
        if (fadePending && fadeInVideo) {
            delegate.getVirtualScreens().forEach(vs -> {
                PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

                if (fader != null) {
                    fader.fadeIn(videoPaintable);
                }
            });

            fadePending = false;
        }
    }

    @Override
    public void paintComponent(GLFWGraphicsAdapter g, VirtualScreen vs) {
        if (finished) {
            return;
        }

        if (!render.get()) {
            return;
        }

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

        if (!fadeInVideo) {
            videoPaintable.paintComponent(g, vs);
            return;
        }

        PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

        if (fader != null) {
            fader.paintComponent(g);
        }
    }

    public int[] getImageData() {
        return imageData;
    }

    public void freeze() {
        if (freeze) {
            return;
        }

        freezeImageData = frameBuffer.peek();

        freeze = true;
        freezes.keySet().forEach(vsId -> freezes.put(vsId, true));

        updateImages();
    }

    public void unfreeze() {
        if (freeze) {
            rebuildLayout(false);
            freeze = false;
        }
    }

    protected void generateBuffer(int w, int h) {
        freeze();
        firstFrame = true;
        videoW = w;
        videoH = h;

        frameBuffer.clear();

        for (int i=0; i<3; i++) {
            frameBuffer.add(new int[w * h]);
        }
    }

    @Override
    public void finish() {
        finished = true;

        this.player.release();

        delegate.getVirtualScreens().forEach(vs -> {
            delegate.runOnProvider(vs, provider -> videoPaintable.freeTex(vs, provider));
        });
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    private final class MyRenderCallback implements RenderCallback {
        MyRenderCallback() {
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            int[] imageData = frameBuffer.poll();
            nativeBuffers[0].asIntBuffer().get(imageData);
            frameBuffer.add(imageData);

            ProjectionVideo.this.imageData = imageData;

            if (firstFrame) {
                firstFrame = false;
            } else {
                unfreeze();
                fadeInVideo();
            }

            updateImages();
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
