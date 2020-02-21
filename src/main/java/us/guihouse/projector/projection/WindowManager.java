package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.*;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.services.SettingsService;
import us.guihouse.projector.utils.BlendGenerator;
import us.guihouse.projector.utils.WindowConfigsLoader;

public class WindowManager implements Runnable, CanvasDelegate, WindowConfigsLoader.WindowConfigsObserver {

    private static final int BLEND_WIDTH_PIXELS = 200;

    private WindowConfigsLoader configLoader;

    private List<WindowConfig> windowConfigs = Collections.emptyList();

    private final PreviewPanel preview;

    private final ProjectionCanvas projectionCanvas;

    private GraphicsDevice defaultDevice;

    private List<ProjectionWindow> windows = Collections.emptyList();

    private BufferedImage whiteImage;
    private HashMap<Integer, BufferedImage> blendAssets = new HashMap<>();
    private HashMap<String, AlphaComposite> blackLevelAssets = new HashMap<>();
    private HashMap<String, AffineTransform> transformAssets = new HashMap<>();

    private Thread drawThread;

    private boolean running = false;
    private boolean fullScreen = false;
    private boolean starting = false;

    private final SettingsService settingsService;

    private final Cursor blankCursor;

    public WindowManager(SettingsService settingsService) {
        this.settingsService = settingsService;

        projectionCanvas = new ProjectionCanvas(this);

        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Create a new blank cursor.
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");

        preview = new PreviewPanel(this);

        this.configLoader = new WindowConfigsLoader(this);
    }

    public ProjectionManager getManager() {
        return projectionCanvas;
    }

    public void setDefaultDevice(GraphicsDevice defaultDevice) {
        this.defaultDevice = defaultDevice;
    }

    private void stopEngine() {
        running = false;

        configLoader.stop();

        if (drawThread != null) {
            drawThread.interrupt();
            drawThread = null;
        }

        windows.forEach(ProjectionWindow::shutdown);

        preview.setProjectionCanvas(null);
    }

    private void startEngine() {
        if (starting) {
            return;
        }

        configLoader.start();

        generateAssets();

        if (!windows.isEmpty()) {
            starting = true;

            windows.forEach(ProjectionWindow::init);
            windows.forEach(w -> w.setCursor(blankCursor));

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    windows.forEach(ProjectionWindow::makeVisible);
                    projectionCanvas.init();
                    preview.setProjectionCanvas(projectionCanvas);

                    running = true;
                    drawThread = new Thread(WindowManager.this);
                    drawThread.setPriority(Thread.MAX_PRIORITY);
                    drawThread.start();
                    starting = false;
                }
            });
        }
    }

    @Override
    public void run() {
        HashMap<String, BufferStrategy> bufferStrategies = new HashMap();

        windows.forEach(window -> {
            bufferStrategies.put(window.getCurrentDevice().getDevice().getIDstring(), window.getFrame().getBufferStrategy());
        });

        HashMap<String, Graphics2D> allGraphics = new HashMap<>();

        while (running) {
            preview.scheduleRepaint();

            windowConfigs.forEach(windowConfig -> {
                BufferStrategy bufferStrategy = bufferStrategies.get(windowConfig.getDisplayId());
                if (bufferStrategy != null) {
                    allGraphics.put(windowConfig.getDisplayId(), (Graphics2D) bufferStrategy.getDrawGraphics());
                }
            });

            windowConfigs.forEach(windowConfig -> {
                Graphics2D g = allGraphics.get(windowConfig.getDisplayId());

                g.setColor(Color.BLACK);
                g.fillRect(windowConfig.getBgFillX(), windowConfig.getBgFillY(), windowConfig.getBgFillWidth(), windowConfig.getBgFillHeight());

                AffineTransform transform = transformAssets.get(windowConfig.getDisplayId());

                g.setTransform(transform);

                projectionCanvas.paintComponent(g);

                g.setTransform(transform);

                Composite bLevel = blackLevelAssets.get(windowConfig.getDisplayId());

                if (bLevel != null) {
                    Composite old = g.getComposite();
                    g.setComposite(bLevel);
                    g.drawImage(whiteImage, windowConfig.getBlackLevelX(), windowConfig.getBlackLevelY(), null);
                }

                windowConfig.getBlends().forEach(blend -> {
                    BufferedImage img = blendAssets.get(blend.getId());
                    if (img != null) {
                        g.drawImage(img, blend.getX() - windowConfig.getX(), blend.getY() - windowConfig.getY(), null);
                    }
                });

                windowConfig.getHelpLines().forEach(helpLine -> {
                    g.setColor(Color.WHITE);
                    g.drawLine(helpLine.getX1(), helpLine.getY1(), helpLine.getX2(), helpLine.getY2());
                });
            });

            bufferStrategies.forEach((displayId, bufferStrategy) -> {
                bufferStrategy.show();
            });

            allGraphics.forEach((displayId, g) -> {
                g.dispose();
            });

            allGraphics.clear();

            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                if (running) {
                    running = false;
                    Logger.getLogger(ProjectionWindow.class.getName()).log(Level.INFO, null, ex);
                }
            }
        }

        drawThread = null;
    }

    @Override
    public int getWidth() {
        return windowConfigs.stream().map(wc -> wc.getX() + wc.getWidth()).max(Integer::compareTo).orElse(800);
    }

    @Override
    public int getHeight() {
        return windowConfigs.stream().map(wc -> wc.getY() + wc.getHeight()).max(Integer::compareTo).orElse(600);
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
        projectionCanvas.finish();
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

    @Override
    public GraphicsDevice getDefaultDevice() {
        return defaultDevice;
    }

    public void setDevices(List<GraphicsFinder.Device> devices) {
        stopEngine();
        this.windows = devices.stream().map(ProjectionWindow::new).collect(Collectors.toList());
        startEngine();
    }

    @Override
    public void updateConfigs(List<WindowConfig> windowConfigs) {
        this.windowConfigs = windowConfigs;
        generateAssets();
    }

    @Override
    public List<WindowConfig> getDefaultConfigs() {
        return this.windows.stream()
                .map(ProjectionWindow::getCurrentDevice)
                .map(device -> {
                    WindowConfig wc = new WindowConfig();
                    wc.setDisplayId(device.getDevice().getIDstring());

                    wc.setWidth(device.getDevice().getDisplayMode().getWidth());
                    wc.setHeight(device.getDevice().getDisplayMode().getHeight());
                    wc.setX(0);
                    wc.setY(0);

                    wc.setBgFillX(0);
                    wc.setBgFillY(0);
                    wc.setBgFillWidth(device.getDevice().getDisplayMode().getWidth());
                    wc.setBgFillHeight(device.getDevice().getDisplayMode().getHeight());

                    wc.setBlackLevelPadding(0);
                    wc.setBlackLevelX(0);
                    wc.setBlackLevelY(0);

                    wc.setBlends(Collections.emptyList());
                    wc.setHelpLines(Collections.emptyList());

                    wc.setScaleX(1.0);
                    wc.setScaleY(1.0);
                    wc.setShearX(0.0);
                    wc.setShearY(0.0);
                    wc.setRotate(0.0);

                    return wc;
                }).collect(Collectors.toList());
    }

    private void generateAssets() {
        blendAssets.clear();
        transformAssets.clear();
        blackLevelAssets.clear();

        if (whiteImage != null) {
            whiteImage.flush();
        }

        int greaterWidth = windowConfigs.stream().map(WindowConfig::getWidth).max(Integer::compareTo).orElse(800);
        int greaterHeight = windowConfigs.stream().map(WindowConfig::getWidth).max(Integer::compareTo).orElse(800);

        whiteImage = new BufferedImage(greaterWidth, greaterHeight, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < greaterWidth; x++) {
            for (int y = 0; y < greaterHeight; y++) {
                whiteImage.setRGB(x, y, 0xFFFFFFFF);
            }
        }

        windowConfigs.forEach(wc -> {
            wc.getBlends().forEach(blend -> {
                blendAssets.put(blend.getId(), BlendGenerator.makeBlender(blend));
            });

            AffineTransform t = new AffineTransform();

            t.translate(wc.getX(), wc.getY());
            t.scale(wc.getScaleX(), wc.getScaleY());
            t.shear(wc.getShearX(), wc.getShearY());
            t.rotate(wc.getRotate());

            transformAssets.put(wc.getDisplayId(), t);

            AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, wc.getBlackLevelPadding());
            blackLevelAssets.put(wc.getDisplayId(), composite);
        });

    }
}
