/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.*;

import javafx.application.Platform;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

/**
 *
 * @author guilherme
 */
public class ProjectionPlayer implements Projectable {

    private final CanvasDelegate delegate;

    private PlayerPanel panel;

    private DirectMediaPlayer player;

    private BufferedImage image;
    private final MediaPlayerFactory factory;

    private int deviceW;
    private int deviceH;

    private int width;
    private int height;

    private int previewOW;
    private int previewOH;
    private int previewW;
    private int previewH;
    private int previewY;

    private int projectionX;
    private int projectionY;

    private GraphicsDevice device;
    private MyRenderCallback renderCallback;

    ProjectionPlayer(CanvasDelegate delegate) {
        this.delegate = delegate;

        factory = new MediaPlayerFactory();
    }

    @Override
    public void paintComponent(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, deviceW, deviceH);
        g.drawImage(image, null, projectionX, projectionY);
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

        this.deviceW = delegate.getWidth();
        this.deviceH = delegate.getHeight();

        renderCallback = new MyRenderCallback(panel);

        this.player = factory.newDirectMediaPlayer(new MyBufferFormatCallback(), renderCallback);
        this.player.setAdjustVideo(true);

    }

    @Override
    public void init() {
        if (panel == null) {
            panel = new PlayerPanel();
            panel.setLayout(new FlowLayout());
        }

        rebuildLayout();
    }

    @Override
    public void finish() {
        this.player.release();
    }

    public DirectMediaPlayer getPlayer() {
        return player;
    }

    public JComponent getPreviewPanel() {
        return panel;
    }

    public void setPreviewPanelSize(double dw, double dh) {
        this.previewOW = (int) Math.round(dw);
        this.previewOH = (int) Math.round(dh);

        panel.setBounds(0,0, previewOW, previewOH);
        panel.repaint();

        recalculatePreviewSize();
    }

    private void recalculatePreviewSize() {
        previewW = previewOW;
        previewH = (int) Math.round((this.height / (double) this.width) * previewOW);

        previewY = (previewOH - previewH) / 2;
    }

    private BufferedImage generateBuffer(int w, int h) {
        float scaleW = deviceW / (float) w;
        float scaleH = deviceH / (float) h;

        float scale = Math.min(scaleW, scaleH);

        this.width = Math.round(scale * w);
        this.height = Math.round(scale * h);

        this.projectionX = (deviceW - this.width) / 2;
        this.projectionY = (deviceH - this.height) / 2;

        image = device.getDefaultConfiguration().createCompatibleImage(width, height);
        image.setAccelerationPriority(1.0f);

        renderCallback.setImage(image);
        panel.setImage(image);

        recalculatePreviewSize();

        return image;
    }

    private final static class MyRenderCallback extends RenderCallbackAdapter {
        private BufferedImage image;
        private JComponent preview;

        MyRenderCallback(JComponent preview) {
            super(new int[0]);
            this.preview = preview;
        }

        public void setImage(BufferedImage image) {
            this.image = image;
        }

        @Override
        public void onDisplay(DirectMediaPlayer mediaPlayer, int[] data) {
            preview.repaint();
        }

        @Override
        public int[] rgbBuffer() {
            return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        }
    }

    private final class MyBufferFormatCallback implements BufferFormatCallback {

        MyBufferFormatCallback() { }

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            BufferedImage img = generateBuffer(sourceWidth, sourceHeight);
            return new RV32BufferFormat(img.getWidth(), img.getHeight());
        }
    }

    private final class PlayerPanel extends JComponent {
        private boolean repainting;
        private BufferedImage image;

        private void setImage(BufferedImage image) {
            this.image = image;
        }

        @Override
        public void repaint() {
            if (repainting) {
                return;
            }

            repainting = true;

            Platform.runLater(() -> {
                super.repaint();
                repainting = false;
            });
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(Color.BLACK);
            g.fillRect(0,0,previewOW, previewOH);
            g.drawImage(image, 0, previewY, previewW, previewH, null);
        }
    }
}
