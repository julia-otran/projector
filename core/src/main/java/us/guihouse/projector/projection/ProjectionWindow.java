/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import lombok.Getter;
import us.guihouse.projector.other.GraphicsFinder;

/**
 *
 * @author guilherme
 */
public class ProjectionWindow  {
    private static final BufferedImage BLANK_CURSOR_IMG = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private static final Cursor BLANK_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(BLANK_CURSOR_IMG, new Point(0, 0), "blank cursor");

    @Getter
    private JFrame frame;

    @Getter
    private final GraphicsFinder.Device currentDevice;
    private BufferStrategy strategy;

    ProjectionWindow(GraphicsFinder.Device device) {
        this.currentDevice = device;
    }

    void init() {
        frame = new JFrame(currentDevice.getDevice().getDefaultConfiguration());

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setLayout(null);
        frame.setCursor(BLANK_CURSOR);

        Rectangle displayRect = currentDevice.getDevice().getDefaultConfiguration().getBounds();
        frame.setBounds(displayRect);
        frame.setBackground(Color.BLACK);
        frame.setIgnoreRepaint(true);
    }

    void updateOutput(BufferedImage src) {
        if (strategy == null) {
            return;
        }

        //do {
            //do {
                Graphics graphics = strategy.getDrawGraphics();

                graphics.drawImage(src, 0, 0, null);

                graphics.dispose();
            //} while (strategy.contentsRestored());

            strategy.show();
        //} while (strategy.contentsLost());
    }

    void makeVisible() {
        frame.setVisible(true);
        frame.createBufferStrategy(1);
        strategy = frame.getBufferStrategy();
    }

    void shutdown() {
        if (frame != null) {
            final Frame f = frame;
            currentDevice.getDevice().setFullScreenWindow(null);

            SwingUtilities.invokeLater(() -> {
                strategy.dispose();
                strategy = null;
                f.setVisible(false);
                f.dispose();
            });

            frame = null;
        }
    }

    public void setFullScreen(boolean fullScreen) {
        if (fullScreen && this.currentDevice.getDevice().isFullScreenSupported()) {
            this.currentDevice.getDevice().setFullScreenWindow(frame);
        } else {
            this.currentDevice.getDevice().setFullScreenWindow(null);
        }
    }
}
