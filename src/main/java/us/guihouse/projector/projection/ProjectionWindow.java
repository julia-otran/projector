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
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import us.guihouse.projector.other.OsCheck;
import us.guihouse.projector.services.SettingsService;

/**
 *
 * @author guilherme
 */
public class ProjectionWindow implements Runnable, CanvasDelegate {

    private JFrame frame;
    private final PreviewPanel preview;
    
    private final ProjectionCanvas projectionCanvas;
    private GraphicsDevice currentDevice;
    private Thread drawThread;
    
    private boolean running = false;
    private boolean fullScreen = false;
    private boolean starting = false;
    
    private final SettingsService settingsService;
    
    private final Cursor blankCursor;
    

    public ProjectionWindow(SettingsService settingsService) {
        this.settingsService = settingsService;
        
        projectionCanvas = new ProjectionCanvas(this);
        
        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Create a new blank cursor.
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        
        preview = new PreviewPanel(this);
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

        if (frame != null) {
            final Frame f = frame;
            
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    f.setVisible(false);
                    f.dispose();
                }
            });
            
            frame = null;
        }
        
        preview.setProjectionCanvas(null);
    }

    private void startEngine() {
        if (starting) {
            return;
        }
        
        if (currentDevice != null) {
            frame = new JFrame(currentDevice.getDefaultConfiguration());
            
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            frame.setUndecorated(true);
            frame.setIgnoreRepaint(true);
            frame.setLayout(null);
            frame.setCursor(blankCursor);
            
            if (OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS) {
                if (fullScreen) {
                    setWindowCanFullScreen(frame, true);
                }
            } else {
                Rectangle displayRect = currentDevice.getDefaultConfiguration().getBounds();
                frame.setBounds(displayRect);
            }
            
            starting = true;
            
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    frame.setVisible(true);
                    
                    if (fullScreen) {
                        currentDevice.setFullScreenWindow(frame);
                    }
                    
                    frame.createBufferStrategy(2);
            
                    projectionCanvas.init();
                    preview.setProjectionCanvas(projectionCanvas);

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
        Graphics2D g;

        while (running) {
            preview.scheduleRepaint();
            
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
        
        drawThread = null;
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
    
    public JPanel getPreviewPanel() {
        return preview;
    }
    
    /**
    * @param window
    */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setWindowCanFullScreen(Window window, boolean can) {
       try {
           Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
           Class params[] = new Class[]{Window.class, Boolean.TYPE};
           Method method = util.getMethod("setWindowCanFullScreen", params);
           method.invoke(util, window, can);
       } catch (ClassNotFoundException ex) {
            Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(ProjectionWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
