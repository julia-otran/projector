/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 *
 * @author guilherme
 */
public class PreviewPanel extends JPanel implements Runnable {
    private final CanvasDelegate delegate;
    private ProjectionCanvas projectionCanvas;
    private boolean repainting = false;
    private boolean running = false;
    private Thread updateThread = null;

    public PreviewPanel(CanvasDelegate delegate) {
        this.delegate = delegate;
        JLabel testLabel = new JLabel();
        testLabel.setText("X");
        add(testLabel);
    }

    void setProjectionCanvas(ProjectionCanvas projectionCanvas) {
        this.projectionCanvas = projectionCanvas;

        if (projectionCanvas == null) {
            running = false;
            if (updateThread != null) {
                try {
                    updateThread.join();
                    updateThread = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (updateThread == null) {
                running = true;
                updateThread = new Thread(this);
                updateThread.start();
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int gw = getWidth();
        int gh = getHeight();

        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gw, gh);

        if (projectionCanvas != null) {
            int dw = delegate.getWidth();
            int dh = delegate.getHeight();

            double scaleX = gw / (double) dw;
            double scaleY = gh / (double) dh;
            double scale = Math.min(scaleX, scaleY);

            int pw = (int) Math.round(dw * scale);
            int ph = (int) Math.round(dh * scale);
            int px = (gw - pw) / 2;
            int py = (gh - ph) / 2;

            AffineTransform old = g2.getTransform();
            g2.translate(px, py);
            g2.scale(scale, scale);
            projectionCanvas.paintComponent(g2);
            g2.setTransform(old);
        }

        repainting = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                running = false;
                e.printStackTrace();
            }

            if (repainting) {
                continue;
            }

            repainting = true;

            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    repaint();
                }
            });
        }
    }
}
