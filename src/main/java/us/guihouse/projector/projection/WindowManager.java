package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
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

    private WindowConfigsLoader configLoader;

    private List<WindowConfig> windowConfigs = Collections.emptyList();

    private final PreviewPanel preview;

    private final ProjectionCanvas projectionCanvas;

    private GraphicsDevice defaultDevice;

    private BufferedImage targetRender;
    private HashMap<String, BufferedImage> bLevelFixed = new HashMap<>();

    private List<ProjectionWindow> windows = Collections.emptyList();

    private HashMap<Integer, BufferedImage> blendAssets = new HashMap<>();
    private HashMap<String, AffineTransform> transformAssets = new HashMap<>();
    private HashMap<String, RescaleOp> blackLevelRescaleOps = new HashMap<>();
    private HashMap<String, BufferedImage> screenImages = new HashMap<>();
    private HashMap<String, Graphics2D> screenGraphics = new HashMap<>();
    private HashMap<String, BufferedImage> outputs = new HashMap<>();
    private HashMap<String, Graphics2D> outputGraphics = new HashMap<>();

    private Thread drawThread;

    private boolean running = false;
    private boolean fullScreen = false;
    private boolean starting = false;
    private boolean drawing = false;
    private final Object sync = new Object();

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

        preview.setProjectionCanvas(null);

        if (drawThread != null) {
            try {
                drawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            drawThread = null;
        }
    }

    private void startEngine() {
        if (starting) {
            return;
        }

        configLoader.start();

        generateAssets();

        starting = true;

        targetRender = getDefaultDevice().getDefaultConfiguration().createCompatibleImage(getWidth(), getHeight());
        targetRender.setAccelerationPriority(1.0f);

        windowConfigs.forEach(wc -> {
            windows
                    .stream()
                    .filter(w -> w.getCurrentDevice().getDevice().getIDstring().equals(wc.getDisplayId()))
                    .findAny()
                    .ifPresent(w -> {
                        w.init();
                        w.setCursor(blankCursor);
                        w.setFullScreen(fullScreen);
                    });
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                windowConfigs.forEach(wc -> {
                    windows
                            .stream()
                            .filter(w -> w.getCurrentDevice().getDevice().getIDstring().equals(wc.getDisplayId()))
                            .findAny()
                            .ifPresent(ProjectionWindow::makeVisible);
                });

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

    @Override
    public void run() {
        HashMap<String, BufferStrategy> bufferStrategies = new HashMap();

        screenGraphics.clear();
        screenImages.clear();

        windowConfigs.forEach(wc -> {
            windows.stream()
                    .filter(w -> w.getCurrentDevice().getDevice().getIDstring().equals(wc.getDisplayId()))
                    .findAny()
                    .ifPresent(window -> {
                        BufferStrategy strategy = window.getBufferStrategy();
                        bufferStrategies.put(wc.getDisplayId(), strategy);

                        GraphicsDevice device = window.getCurrentDevice().getDevice();
                        int width = device.getDisplayMode().getWidth();
                        int height = device.getDisplayMode().getHeight();

                        BufferedImage screenImage = device.getDefaultConfiguration().createCompatibleImage(width, height);
                        Graphics2D screenGraphic = screenImage.createGraphics();

                        screenGraphics.put(wc.getDisplayId(), screenGraphic);
                        screenImages.put(wc.getDisplayId(), screenImage);

                        BufferedImage bLevelFixedImg = getDefaultDevice().getDefaultConfiguration().createCompatibleImage(getWidth(), getHeight());
                        bLevelFixedImg.setAccelerationPriority(1.0f);

                        bLevelFixed.put(wc.getDisplayId(), bLevelFixedImg);

                        BufferedImage outputImage = device.getDefaultConfiguration().createCompatibleImage(width, height);
                        Graphics2D outputGraphic = outputImage.createGraphics();

                        outputs.put(wc.getDisplayId(), outputImage);
                        outputGraphics.put(wc.getDisplayId(), outputGraphic);
                    });
        });

        Graphics2D targetGraphics = targetRender.createGraphics();

        while (running) {
            projectionCanvas.paintComponent(targetGraphics);

            windowConfigs.forEach(windowConfig -> {
                BufferedImage bLevelFix = bLevelFixed.get(windowConfig.getDisplayId());
                RescaleOp rescaleOp = blackLevelRescaleOps.get(windowConfig.getDisplayId());
                rescaleOp.filter(targetRender, bLevelFix);
            });

            windowConfigs.forEach(windowConfig -> {
                Graphics2D g = screenGraphics.get(windowConfig.getDisplayId());
                BufferedImage bLevelFix = bLevelFixed.get(windowConfig.getDisplayId());

                if (g == null) {
                    return;
                }

                g.setColor(Color.BLACK);
                g.fillRect(windowConfig.getBgFillX(), windowConfig.getBgFillY(), windowConfig.getBgFillWidth(), windowConfig.getBgFillHeight());

                AffineTransform transform = transformAssets.get(windowConfig.getDisplayId());

                g.setTransform(transform);

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g.drawImage(bLevelFix, 0,0, null);

                g.setTransform(transform);
                g.translate(windowConfig.getX(), windowConfig.getY());

                windowConfig.getBlends().forEach(blend -> {
                    BufferedImage img = blendAssets.get(blend.getId());
                    if (img != null) {
                        g.drawImage(img, blend.getX(), blend.getY(), null);
                    }
                });

                windowConfig.getHelpLines().forEach(helpLine -> {
                    g.setColor(Color.WHITE);
                    g.drawLine(helpLine.getX1(), helpLine.getY1(), helpLine.getX2(), helpLine.getY2());
                });
            });

            synchronized (sync) {
                if (drawing) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                drawing = true;
            }

            windowConfigs.forEach(windowConfig -> {
                BufferedImage screen = screenImages.get(windowConfig.getDisplayId());
                Graphics2D g = outputGraphics.get(windowConfig.getDisplayId());
                g.drawImage(screen, 0, 0, null);
            });

            SwingUtilities.invokeLater(() -> {
                bufferStrategies.forEach((displayId, bufferStrategy) -> {
                    Graphics g = bufferStrategy.getDrawGraphics();
                    BufferedImage screenImage = outputs.get(displayId);

                    g.drawImage(screenImage, 0, 0, null);
                    bufferStrategy.show();
                    g.dispose();
                });

                synchronized (sync) {
                    drawing = false;
                    sync.notify();
                }
            });
        }

        windows.forEach(ProjectionWindow::shutdown);
        targetGraphics.dispose();
        screenGraphics.forEach((id, g) -> {
            g.dispose();
        });

        drawThread = null;
    }

    @Override
    public int getWidth() {
        int screenStart = windowConfigs.stream().map(wc -> wc.getX()).min(Integer::compareTo).orElse(0);
        int screenEnd = windowConfigs.stream().map(wc -> wc.getX() + wc.getWidth()).max(Integer::compareTo).orElse(800);
        return screenEnd - screenStart;
    }

    @Override
    public int getHeight() {
        int screenStart = windowConfigs.stream().map(wc -> wc.getY()).min(Integer::compareTo).orElse(0);
        int screenEnd = windowConfigs.stream().map(wc -> wc.getY() + wc.getHeight()).max(Integer::compareTo).orElse(600);
        return screenEnd - screenStart;
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

                    wc.setBLevelOffset(0);
                    wc.setBLevelScaleFactor(1.0f);

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

        windowConfigs.forEach(wc -> {
            wc.getBlends().forEach(blend -> {
                blendAssets.put(blend.getId(), BlendGenerator.makeBlender(blend));
            });

            AffineTransform t = new AffineTransform();

            t.translate(-1 * wc.getX(), -1 * wc.getY());
            t.scale(wc.getScaleX(), wc.getScaleY());
            t.shear(wc.getShearX(), wc.getShearY());
            t.rotate(wc.getRotate());

            transformAssets.put(wc.getDisplayId(), t);

            blackLevelRescaleOps.put(wc.getDisplayId(), new RescaleOp(wc.getBLevelScaleFactor(), wc.getBLevelOffset(), null));
        });

    }
}
