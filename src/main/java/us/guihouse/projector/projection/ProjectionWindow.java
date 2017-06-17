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

/**
 *
 * @author guilherme
 */
public class ProjectionWindow implements Runnable, CanvasDelegate {
    private Frame frame;
    private final ProjectionCanvas projectionCanvas;
    private GraphicsDevice currentDevice;
    private Thread drawThread;
    private boolean running = false;
    private boolean fullScreen = false;
    
    private final List<Frame> pending;
    
    public ProjectionWindow() {
        pending = new ArrayList<>();
        projectionCanvas = new ProjectionCanvas(this);
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
            disposeFrame(frame);            
            frame = null;
        }
    }
    
    private void startEngine() {
        if (currentDevice != null) {
            frame = new Frame(currentDevice.getDefaultConfiguration());
            
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            frame.setBounds(currentDevice.getDefaultConfiguration().getBounds());
            frame.setUndecorated(true);
            frame.setIgnoreRepaint(true);
            
            if (fullScreen) {
                currentDevice.setFullScreenWindow(frame);
            } else {
                frame.setVisible(true);
            }
            
            frame.createBufferStrategy(2);

            projectionCanvas.init();

            running = true;
            drawThread = new Thread(this);
            drawThread.start();
        }
    }
    
    @Override
    public void run() {
        BufferStrategy bufferStrategy = frame.getBufferStrategy();
        Graphics g;
        
        while (running) {
            g = bufferStrategy.getDrawGraphics();
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

    public void dispose() {
        pending.forEach(f -> f.dispose());
    }
    
    public void stop() {
        stopEngine();
    }

    private void disposeFrame(final Frame fr) {
        pending.add(fr);
    }
}
