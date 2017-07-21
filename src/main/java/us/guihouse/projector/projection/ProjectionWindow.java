/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javafx.geometry.Bounds;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import us.guihouse.projector.services.SettingsService;

/**
 *
 * @author guilherme
 */
public class ProjectionWindow implements Runnable, CanvasDelegate {

    private Frame frame;
    private Frame previewFrame;
    
    private final ProjectionCanvas projectionCanvas;
    private GraphicsDevice currentDevice;
    private Thread drawThread;
    
    private boolean running = false;
    private boolean fullScreen = false;
    private boolean starting = false;
    
    private final SettingsService settingsService;
    
    private final Cursor blankCursor;
    
    private float previewScale;

    public ProjectionWindow(SettingsService settingsService) {
        this.settingsService = settingsService;
        
        projectionCanvas = new ProjectionCanvas(this);
        
        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Create a new blank cursor.
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        
        previewScale = 1;
        createPreviewFrame();
    }

    public ProjectionManager getManager() {
        return projectionCanvas;
    }

    public void setDevice(GraphicsDevice device) {
        stopEngine();
        this.currentDevice = device;
        startEngine();
    }

    private void stopEngine() {
        running = false;

        if (drawThread != null) {
            drawThread.interrupt();
            drawThread = null;
        }

        if (currentDevice != null) {
            currentDevice.setFullScreenWindow(null);
        }

        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
            frame = null;
        }
        
        previewFrame.setVisible(false);
    }

    private void startEngine() {
        if (starting) {
            return;
        }
        
        if (currentDevice != null) {
            frame = new JFrame(currentDevice.getDefaultConfiguration());
            
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);
            frame.setIgnoreRepaint(true);
            frame.setLayout(null);
            frame.setCursor(blankCursor);
            
            if (fullScreen) {
                currentDevice.setFullScreenWindow(frame);
            } else {
                Rectangle displayRect = currentDevice.getDefaultConfiguration().getBounds();
                frame.setLocation(displayRect.getLocation());
                frame.setSize(displayRect.getSize());
            }

            frame.setVisible(true);
            previewFrame.setVisible(true);
            
            frame.createBufferStrategy(2);
            previewFrame.createBufferStrategy(2);
            
            starting = true;
            
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    projectionCanvas.init();

                    running = true;
                    drawThread = new Thread(ProjectionWindow.this);
                    drawThread.start();
                    starting = false;
                }
            });
        }
    }

    @Override
    public void run() {
        BufferStrategy bufferStrategy = frame.getBufferStrategy();
        BufferStrategy previewStrategy = previewFrame.getBufferStrategy();
        
        Graphics2D g;
        Graphics2D gp;

        AffineTransform oldTransform;
        
        while (running) {
            gp = (Graphics2D) previewStrategy.getDrawGraphics();
            
            oldTransform = gp.getTransform();
            gp.scale(previewScale, previewScale);
            projectionCanvas.paintComponent(gp);
            gp.setTransform(oldTransform);
            
            previewStrategy.show();
            gp.dispose();
            
            g = (Graphics2D) bufferStrategy.getDrawGraphics();
            projectionCanvas.paintComponent(g);
            bufferStrategy.show();
            g.dispose();

            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                if (running) {
                    running = false;
                    Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public int getWidth() {
        if (frame == null) {
            return 800;
        }

        return frame.getWidth();
    }

    @Override
    public int getHeight() {
        if (frame == null) {
            return 600;
        }

        return frame.getHeight();
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        if (this.fullScreen == fullScreen) {
            return;
        }

        stopEngine();
        this.fullScreen = fullScreen;
        startEngine();
    }

    public void stop() {
        stopEngine();
    }

    @Override
    public SettingsService getSettingsService() {
        return settingsService;
    }
    
    public void updatePreviewSize(int x, int y, int maxWidth, int maxHeight) {
        int width = maxWidth;
        float scale = width / (float) getWidth();
        int height = Math.round(getHeight() * scale);
        
        if (height > maxHeight) {
            height = maxHeight;
            scale = height / (float) getHeight();
            width = Math.round(getWidth() * scale);
        }
        
        previewScale = scale;
        x += (maxWidth - width) / 2;
        y += (maxHeight - height) / 2;
        
        previewFrame.setLocation(x, y);
        previewFrame.setSize(width, height);
    }

    private void createPreviewFrame() {
        previewFrame = new Frame();
        previewFrame.setUndecorated(true);
        previewFrame.setIgnoreRepaint(true);
        previewFrame.setLayout(null);
        previewFrame.setAlwaysOnTop(true);
        previewFrame.setResizable(false);
    }

    public void stopPreview() {
        previewFrame.dispose();
    }
}
