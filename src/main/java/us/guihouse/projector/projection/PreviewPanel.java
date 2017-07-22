/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author guilherme
 */
public class PreviewPanel extends JPanel {
    private final CanvasDelegate delegate;
    private ProjectionCanvas projectionCanvas;
    private boolean repainting = false;

    public PreviewPanel(CanvasDelegate delegate) {
        this.delegate = delegate;
    }
    
    ProjectionCanvas getProjectionCanvas() {
        return projectionCanvas;
    }

    void setProjectionCanvas(ProjectionCanvas projectionCanvas) {
        this.projectionCanvas = projectionCanvas;
    }
    
    @Override
    public void paint(Graphics g) {
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

    void scheduleRepaint() {
        if (repainting) {
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                repaint();
            }
        });
    }
}
