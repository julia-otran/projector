package us.guihouse.projector.projection;

import lombok.Getter;
import lombok.Setter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;
import us.guihouse.projector.utils.VlcPlayerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class ProjectionVideo implements Projectable {
    private final CanvasDelegate delegate;

    protected DirectMediaPlayer player;

    protected BufferedImage image;

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

        renderCallback = new ProjectionVideo.MyRenderCallback();

        this.player = VlcPlayerFactory.getFactory().newDirectMediaPlayer(new ProjectionVideo.MyBufferFormatCallback(), renderCallback);
        this.player.setAdjustVideo(true);
    }

    @Override
    public void init() {
        rebuildLayout();
    }

    @Override
    public void paintComponent(Graphics2D g) {
        if (render) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, deviceW, deviceH);
            g.drawImage(image, null, projectionX, projectionY);
        }
    }

    protected BufferedImage generateBuffer(int w, int h) {
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

        this.projectionX = (deviceW - this.width) / 2;
        this.projectionY = (deviceH - this.height) / 2;

        image = device.getDefaultConfiguration().createCompatibleImage(width, height);
        image.setAccelerationPriority(1.0f);

        renderCallback.setImage(image);

        return image;
    }

    @Override
    public void finish() {
        this.player.release();
    }

    public DirectMediaPlayer getPlayer() {
        return player;
    }

    public final static class MyRenderCallback extends RenderCallbackAdapter {

        private BufferedImage image;
        private JComponent preview;

        MyRenderCallback() {
            super(new int[0]);
        }

        public void setImage(BufferedImage image) {
            this.image = image;
        }

        public void setPreview(JComponent preview) {
            this.preview = preview;
        }

        @Override
        public void onDisplay(DirectMediaPlayer mediaPlayer, int[] data) {
            if (preview != null) {
                preview.repaint();
            }
        }

        @Override
        public int[] rgbBuffer() {
            return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        }
    }

    private final class MyBufferFormatCallback implements BufferFormatCallback {

        MyBufferFormatCallback() {
        }

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            BufferedImage img = generateBuffer(sourceWidth, sourceHeight);
            return new RV32BufferFormat(img.getWidth(), img.getHeight());
        }
    }
}
