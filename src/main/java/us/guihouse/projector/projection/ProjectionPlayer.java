/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javafx.application.Platform;
import javax.swing.*;
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
public class ProjectionPlayer extends ProjectionVideo {
    private ProjectionPlayer.PlayerPanel panel;

    private int previewOW;
    private int previewOH;
    private int previewW;
    private int previewH;
    private int previewY;

    public ProjectionPlayer(CanvasDelegate delegate) {
        super(delegate, false);
    }

    @Override
    public void init() {
        if (panel == null) {
            panel = new PlayerPanel();
            panel.setLayout(new FlowLayout());
        }

        super.init();
    }

    public JComponent getPreviewPanel() {
        return panel;
    }

    public void setPreviewPanelSize(double dw, double dh) {
        this.previewOW = (int) Math.round(dw);
        this.previewOH = (int) Math.round(dh);

        panel.setBounds(0, 0, previewOW, previewOH);
        panel.repaint();

        recalculatePreviewSize();
    }

    @Override
    protected BufferedImage generateBuffer(int w, int h) {
        BufferedImage buffer = super.generateBuffer(w, h);

        renderCallback.setPreview(panel);
        recalculatePreviewSize();

        return buffer;
    }

    private void recalculatePreviewSize() {
        previewW = previewOW;
        previewH = (int) Math.round((this.height / (double) this.width) * previewOW);

        previewY = (previewOH - previewH) / 2;
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
            g.fillRect(0, 0, previewOW, previewOH);
            g.drawImage(image, 0, previewY, previewW, previewH, null);
        }
    }
}
