package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.other.RuntimeProperties;
import us.guihouse.projector.projection.glfw.GLFWHelper;
import us.guihouse.projector.projection.glfw.GLFWWindowBuilder;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.services.SettingsService;
import us.guihouse.projector.utils.BlendGenerator;
import us.guihouse.projector.utils.WindowConfigsLoader;

public class WindowManager implements Runnable, CanvasDelegate, WindowConfigsLoader.WindowConfigsObserver {

    private final WindowConfigsLoader configLoader;

    private List<WindowConfig> windowConfigs = Collections.emptyList();

    private final PreviewImageView preview;

    private final ProjectionCanvas projectionCanvas;

    private GraphicsDevice defaultDevice;

    private final List<VirtualScreen> virtualScreens = new ArrayList<>();
    private final HashMap<String, BufferedImage> virtualScreensRender = new HashMap<>();
    private final HashMap<String, Graphics2D> virtualScreensGraphics = new HashMap<>();

    private final HashMap<String, ProjectionWindow> windows = new HashMap<>();
    private final HashMap<String, AffineTransform> transformAssets = new HashMap<>();
    private final HashMap<String, BufferedImage> screenImages = new HashMap<>();
    private final HashMap<String, Graphics2D> screenGraphics = new HashMap<>();

    private Thread drawThread;

    @Getter
    @Setter
    private Runnable initializationCallback;

    private boolean running = false;
    private boolean starting = false;

    private final SettingsService settingsService;

    public WindowManager(SettingsService settingsService) {
        this.settingsService = settingsService;

        projectionCanvas = new ProjectionCanvas(this);

        preview = new PreviewImageView(this);

        configLoader = new WindowConfigsLoader(this);
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

        screenGraphics.clear();
        screenImages.clear();

        windowConfigs.forEach(wc -> {
            ProjectionWindow w = windows.get(wc.getDisplayId());

            if (w != null) {
                GraphicsDevice device = w.getCurrentDevice().getDevice();
                int width = device.getDefaultConfiguration().getBounds().width;
                int height = device.getDefaultConfiguration().getBounds().height;

                BufferedImage screenImage = device.getDefaultConfiguration().createCompatibleImage(width, height);
                screenImage.setAccelerationPriority(1.0f);
                Graphics2D screenGraphic = screenImage.createGraphics();

                screenGraphics.put(wc.getDisplayId(), screenGraphic);
                screenImages.put(wc.getDisplayId(), screenImage);
            }
        });

        projectionCanvas.init();
        preview.setProjectionCanvas(projectionCanvas);

        running = true;
        drawThread = new Thread(WindowManager.this);
        drawThread.start();
        starting = false;
    }

    @Override
    public void run() {
        int frames = 0;
        long timestamp = System.nanoTime();

        GLFWHelper.initGLFW();

        GLFWHelper.invokeLater(() -> {
            windowConfigs.forEach(wc -> {
                ProjectionWindow w = windows.get(wc.getDisplayId());

                if (w != null) {
                    w.init(wc);
                    w.makeVisible();
                }
            });

            if (initializationCallback != null) {
                initializationCallback.run();
            }
        });

        while (running) {
            frames++;

            if (RuntimeProperties.isLogFPS()) {
                long newTimestamp = System.nanoTime();
                if (newTimestamp - timestamp > 1000000000) {
                    System.out.println("Virtual Screen FPS=" + frames);
                    frames = 0;
                    timestamp = newTimestamp;
                }
            }

            virtualScreens
                .parallelStream()
                .forEach(virtualScreen -> {
                    Graphics2D targetGraphics = virtualScreensGraphics.get(virtualScreen.getVirtualScreenId());
                    projectionCanvas.paintComponent(targetGraphics, virtualScreen);
                });

            windowConfigs.parallelStream().forEach(windowConfig -> {
                Graphics2D g = screenGraphics.get(windowConfig.getDisplayId());

                if (g == null) {
                    return;
                }

                g.setColor(Color.BLACK);
                g.fillRect(windowConfig.getBgFillX(), windowConfig.getBgFillY(), windowConfig.getBgFillWidth(), windowConfig.getBgFillHeight());

                AffineTransform transform = transformAssets.get(windowConfig.getDisplayId());

                if (transform != null) {
                    g.setTransform(transform);
                }

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g.drawImage(virtualScreensRender.get(windowConfig.getVirtualScreenId()), 0, 0, null);

                if (transform != null) {
                    g.setTransform(transform);
                }

                g.translate(windowConfig.getX(), windowConfig.getY());

                Stroke previousStroke = g.getStroke();
                g.setStroke(new BasicStroke(2.0f));
                g.setColor(Color.WHITE);

                windowConfig.getHelpLines()
                        .forEach(helpLine -> g.drawLine(helpLine.getX1(), helpLine.getY1(), helpLine.getX2(), helpLine.getY2()));

                g.setStroke(previousStroke);
            });

            windowConfigs.forEach(windowConfig -> {
                BufferedImage screen = screenImages.get(windowConfig.getDisplayId());
                ProjectionWindow pw = windows.get(windowConfig.getDisplayId());
                if (pw != null) {
                    pw.updateOutput(screen);
                }
            });

            Thread.yield();
        }

        GLFWHelper.invokeLater(() -> windows.values().forEach(ProjectionWindow::shutdown));

        GLFWHelper.finish();

        screenGraphics.forEach((id, g) -> g.dispose());

        drawThread = null;
    }

    @Override
    public int getMainWidth() {
        return virtualScreens.stream()
                .filter(VirtualScreen::isMainScreen)
                .map(VirtualScreen::getWidth)
                .findFirst()
                .orElse(1280);
    }

    @Override
    public int getMainHeight() {
        return virtualScreens.stream()
                .filter(VirtualScreen::isMainScreen)
                .map(VirtualScreen::getHeight)
                .findFirst()
                .orElse(720);
    }

    @Override
    public List<VirtualScreen> getVirtualScreens() {
        return virtualScreens;
    }

    @Override
    public void setFullScreen(boolean fullScreen) {

    }

    public void stop() {
        stopEngine();
        projectionCanvas.finish();
    }

    @Override
    public SettingsService getSettingsService() {
        return settingsService;
    }

    public ImageView getPreviewPanel() {
        return preview;
    }

    @Override
    public GraphicsDevice getDefaultDevice() {
        return defaultDevice;
    }
    
    public WindowConfigsLoader getWindowConfigsLoader() {
        return configLoader;
    }

    public void setDevices(List<GraphicsFinder.Device> devices) {
        stopEngine();
        this.windows.clear();

        devices.forEach(device -> this.windows.put(device.getDevice().getIDstring(), GLFWWindowBuilder.createWindow(device)));

        startEngine();
    }

    @Override
    public void updateConfigs(List<WindowConfig> windowConfigs) {
        List<WindowConfig> newWindowConfigs = filterWindowConfigs(windowConfigs);

        if (running) {

            boolean quickReload = this.windowConfigs != null && this.windowConfigs.size() == newWindowConfigs.size();

            if (quickReload) {
                for (int i=0; i<newWindowConfigs.size() && quickReload; i++) {
                    WindowConfig current = this.windowConfigs.get(i);
                    WindowConfig newConfig = windowConfigs.get(i);
                    quickReload = current.allowQuickReload(newConfig);
                }
            }

            if (quickReload) {
                loadWindowConfigs(newWindowConfigs);
                generateMutableAssets();

                GLFWHelper.invokeLater(() -> {
                    this.windowConfigs.forEach(windowConfig -> {
                        ProjectionWindow pw = windows.get(windowConfig.getDisplayId());
                        pw.updateWindowConfig(windowConfig);
                    });
                });
            } else {
                stopEngine();
                loadWindowConfigs(newWindowConfigs);
                startEngine();
            }
        } else {
            loadWindowConfigs(newWindowConfigs);
        }
    }

    private List<WindowConfig> filterWindowConfigs(List<WindowConfig> windowConfigs) {
        return windowConfigs.stream().filter(WindowConfig::isProject).collect(Collectors.toList());
    }

    private void loadWindowConfigs(List<WindowConfig> windowConfigs) {
        this.windowConfigs = windowConfigs.stream()
                .peek(wc -> {
                    if (wc.getVirtualScreenId() == null || wc.getVirtualScreenId().isBlank()) {
                        wc.setVirtualScreenId(VirtualScreen.MAIN_SCREEN_ID);
                    }

                    if (wc.getDisplayBounds() != null) {
                        this.windows.forEach((id, w) -> {
                            Rectangle deviceBounds = w.getCurrentDevice().getDevice().getDefaultConfiguration().getBounds();
                            if (deviceBounds.equals(wc.getDisplayBounds())) {
                                wc.setDisplayId(id);
                            }
                        });
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WindowConfig> getDefaultConfigs() {
        return this.windows.values().stream()
                .map(ProjectionWindow::getCurrentDevice)
                .map(device -> {
                    WindowConfig wc = new WindowConfig();
                    wc.setDisplayId(device.getDevice().getIDstring());
                    wc.setVirtualScreenId(VirtualScreen.MAIN_SCREEN_ID);
                    wc.setProject(device.isProjectionDevice());
                    wc.setDisplayBounds(device.getDevice().getDefaultConfiguration().getBounds());

                    wc.setWidth(device.getDevice().getDefaultConfiguration().getBounds().width);
                    wc.setHeight(device.getDevice().getDefaultConfiguration().getBounds().height);
                    wc.setX(0);
                    wc.setY(0);

                    wc.setBgFillX(0);
                    wc.setBgFillY(0);
                    wc.setBgFillWidth(device.getDevice().getDefaultConfiguration().getBounds().width);
                    wc.setBgFillHeight(device.getDevice().getDefaultConfiguration().getBounds().height);

                    wc.setBlackLevelAdjust(null);
                    wc.setWhiteBalance(null);
                    wc.setColorBalance(null);

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

    private void generateMutableAssets() {
        transformAssets.clear();

        windowConfigs.forEach(wc -> {
            AffineTransform t = new AffineTransform();

            t.translate(-1 * wc.getX(), -1 * wc.getY());
            t.scale(wc.getScaleX(), wc.getScaleY());
            t.shear(wc.getShearX(), wc.getShearY());
            t.rotate(wc.getRotate());

            transformAssets.put(wc.getDisplayId(), t);
        });
    }

    private void generateAssets() {
        generateMutableAssets();

        virtualScreens.clear();
        virtualScreensRender.clear();
        virtualScreensGraphics.forEach((id, graphics) -> graphics.dispose());
        virtualScreensGraphics.clear();

        windowConfigs
                .stream()
                .collect(Collectors.groupingBy(WindowConfig::getVirtualScreenId))
                .forEach((virtualScreenId, windowConfigs) -> {
                    VirtualScreen vs = new VirtualScreen();

                    vs.setVirtualScreenId(virtualScreenId);
                    vs.setWindows(windowConfigs);

                    int screenXStart = windowConfigs.stream().map(WindowConfig::getX).min(Integer::compareTo).orElse(0);
                    int screenXEnd = windowConfigs.stream().map(wc -> wc.getX() + wc.getWidth()).max(Integer::compareTo).orElse(1280);
                    vs.setWidth(screenXEnd - screenXStart);

                    int screenYStart = windowConfigs.stream().map(WindowConfig::getY).min(Integer::compareTo).orElse(0);
                    int screenYEnd = windowConfigs.stream().map(wc -> wc.getY() + wc.getHeight()).max(Integer::compareTo).orElse(720);
                    vs.setHeight(screenYEnd - screenYStart);

                    virtualScreens.add(vs);

                    BufferedImage render = getDefaultDevice().getDefaultConfiguration().createCompatibleImage(vs.getWidth(), vs.getHeight());
                    render.setAccelerationPriority(1.0f);

                    virtualScreensRender.put(virtualScreenId, render);
                    virtualScreensGraphics.put(virtualScreenId, render.createGraphics());
                });

    }
}
