/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lombok.Getter;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.other.OsCheck;
import us.guihouse.projector.services.SettingsService;

/**
 *
 * @author guilherme
 */
public class ProjectionWindow  {
    @Getter
    private JFrame frame;

    private Canvas canvas;

    @Getter
    private GraphicsFinder.Device currentDevice;

    ProjectionWindow(GraphicsFinder.Device device) {
        this.currentDevice = device;
    }

    void init() {
        frame = new JFrame(currentDevice.getDevice().getDefaultConfiguration());

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setIgnoreRepaint(true);
        frame.setLayout(null);

        Rectangle displayRect = currentDevice.getDevice().getDefaultConfiguration().getBounds();
        frame.setBounds(displayRect);
        frame.setBackground(Color.BLACK);
    }

    void makeVisible() {
        frame.setVisible(true);
        frame.createBufferStrategy(2);
    }

    void setCursor(final Cursor cursor) {
        frame.setCursor(cursor);
    }

    void shutdown() {
        if (frame != null) {
            final Frame f = frame;
            currentDevice.getDevice().setFullScreenWindow(null);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    f.setVisible(false);
                    f.dispose();
                }
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

    public BufferStrategy getBufferStrategy() {
        return frame.getBufferStrategy();
    }
}
