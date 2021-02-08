package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.other.RuntimeProperties;
import us.guihouse.projector.projection.glfw.GLFWHelper;
import us.guihouse.projector.projection.glfw.GLFWVirtualScreen;
import us.guihouse.projector.projection.glfw.GLFWWindow;
import us.guihouse.projector.projection.glfw.GLFWWindowBuilder;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.services.SettingsService;
import us.guihouse.projector.utils.WindowConfigsLoader;

public class WindowManager implements Runnable, CanvasDelegate, WindowConfigsLoader.WindowConfigsObserver {

    private final WindowConfigsLoader configLoader;

    private List<WindowConfig> windowConfigs = Collections.emptyList();

    private final PreviewImageView preview;

    private final ProjectionCanvas projectionCanvas;

    private GraphicsDevice defaultDevice;

    private final HashMap<String, VirtualScreen> virtualScreens = new HashMap<>();
    private final HashMap<String, BufferedImage> virtualScreensRender = new HashMap<>();
    private final HashMap<String, Graphics2D> virtualScreensGraphics = new HashMap<>();
    private final HashMap<String, GLFWVirtualScreen> glfwVirtualScreens = new HashMap<>();

    private final HashMap<String, GLFWWindow> windows = new HashMap<>();
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

        glfwVirtualScreens.values().forEach(GLFWVirtualScreen::shutdown);

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

        projectionCanvas.init();
        preview.setProjectionCanvas(projectionCanvas);

        running = true;
        drawThread = new Thread(WindowManager.this);

        virtualScreens.forEach((id, virtualScreen) -> {
            HashMap<String, GLFWWindow> vsWindows = new HashMap<>();

            Map<String, WindowConfig> vsWindowConfigs = windowConfigs.stream()
                    .filter(wc -> id.equals(wc.getVirtualScreenId()))
                    .collect(Collectors.toMap(WindowConfig::getDisplayId, Function.identity()));

            vsWindowConfigs.keySet().forEach(displayID -> vsWindows.put(displayID, windows.get(displayID)));

            GLFWVirtualScreen glfwVirtualScreen = new GLFWVirtualScreen(virtualScreen, vsWindows, vsWindowConfigs);
            glfwVirtualScreens.put(id, glfwVirtualScreen);
        });

        GLFWHelper.invokeLater(() -> {
            glfwVirtualScreens.values().forEach(GLFWVirtualScreen::init);

            drawThread.start();
            starting = false;
        });
    }

    @Override
    public void run() {
        if (initializationCallback != null) {
            initializationCallback.run();
        }

        int frames = 0;
        long timestamp = System.nanoTime();

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
                .entrySet()
                .parallelStream()
                .forEach(entry -> {
                    Graphics2D targetGraphics = virtualScreensGraphics.get(entry.getKey());
                    projectionCanvas.paintComponent(targetGraphics, entry.getValue());
                    GLFWVirtualScreen glfwVs = glfwVirtualScreens.get(entry.getKey());
                    if (glfwVs != null) {
                        glfwVs.updateImage(virtualScreensRender.get(entry.getKey()));
                    }
                });


            Thread.yield();
        }

        screenGraphics.forEach((id, g) -> g.dispose());

        drawThread = null;
    }

    @Override
    public int getMainWidth() {
        return virtualScreens.values()
                .stream()
                .filter(VirtualScreen::isMainScreen)
                .map(VirtualScreen::getWidth)
                .findFirst()
                .orElse(1280);
    }

    @Override
    public int getMainHeight() {
        return virtualScreens.values()
                .stream()
                .filter(VirtualScreen::isMainScreen)
                .map(VirtualScreen::getHeight)
                .findFirst()
                .orElse(720);
    }

    @Override
    public List<VirtualScreen> getVirtualScreens() {
        return new ArrayList<>(virtualScreens.values());
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
        List<WindowConfig> newWindowConfigs = prepareWindowConfigs(windowConfigs);

        if (running) {
            boolean quickReload = this.windowConfigs != null &&
                    this.windowConfigs.size() == newWindowConfigs.size() &&
                    this.windowConfigs.stream().allMatch(wc -> newWindowConfigs.stream().anyMatch(wc::allowQuickReload));

            if (quickReload) {
                this.windowConfigs = newWindowConfigs;
                glfwVirtualScreens.values().forEach(vs -> vs.updateWindowConfigs(newWindowConfigs));
            } else {
                stopEngine();
                this.windowConfigs = newWindowConfigs;
                startEngine();
            }
        } else {
            this.windowConfigs = newWindowConfigs;
        }
    }

    private List<WindowConfig> prepareWindowConfigs(List<WindowConfig> windowConfigs) {
        return windowConfigs.stream()
                .filter(WindowConfig::isProject)
                .map(wc -> {
                    WindowConfig.WindowConfigBuilder builder = wc.toBuilder();

                    if (wc.getVirtualScreenId() == null || wc.getVirtualScreenId().isBlank()) {
                        builder = builder.virtualScreenId(VirtualScreen.MAIN_SCREEN_ID);
                    }

                    if (wc.getDisplayBounds() != null) {
                        for (Map.Entry<String, GLFWWindow> entry  : this.windows.entrySet()) {
                            Rectangle deviceBounds = entry.getValue().getCurrentDevice().getDevice().getDefaultConfiguration().getBounds();
                            if (deviceBounds.equals(wc.getDisplayBounds())) {
                                builder = builder.displayId(entry.getKey());
                            }
                        }
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WindowConfig> getDefaultConfigs() {
        return this.windows.values().stream()
                .map(GLFWWindow::getCurrentDevice)
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

                    wc.setBlackLevelAdjust(null);
                    wc.setWhiteBalance(null);
                    wc.setColorBalance(null);

                    wc.setBlends(Collections.emptyList());
                    wc.setHelpLines(Collections.emptyList());

                    return wc;
                }).collect(Collectors.toList());
    }

    private void generateAssets() {
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

                    virtualScreens.put(vs.getVirtualScreenId(), vs);

                    BufferedImage render = getDefaultDevice().getDefaultConfiguration().createCompatibleImage(vs.getWidth(), vs.getHeight());
                    render.setAccelerationPriority(1.0f);

                    virtualScreensRender.put(virtualScreenId, render);
                    virtualScreensGraphics.put(virtualScreenId, render.createGraphics());
                });

    }
}
