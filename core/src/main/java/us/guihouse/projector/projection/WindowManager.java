package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.services.SettingsService;
import us.guihouse.projector.utils.BlendGenerator;
import us.guihouse.projector.utils.WindowConfigsLoader;

public class WindowManager implements Runnable, CanvasDelegate, WindowConfigsLoader.WindowConfigsObserver {

    private WindowConfigsLoader configLoader;

    private List<WindowConfig> windowConfigs = Collections.emptyList();

    private final PreviewImageView preview;

    private final ProjectionCanvas projectionCanvas;

    private final WindowManagerPresenter managerPresenter = new WindowManagerPresenter();

    private GraphicsDevice defaultDevice;

    private BufferedImage targetRender;
    private HashMap<String, BufferedImage> bLevelFixAssets = new HashMap<>();

    private HashMap<String, ProjectionWindow> windows = new HashMap<>();
    private HashMap<Integer, BufferedImage> blendAssets = new HashMap<>();
    private HashMap<String, AffineTransform> transformAssets = new HashMap<>();
    private HashMap<String, BufferedImage> screenImages = new HashMap<>();
    private HashMap<String, Graphics2D> screenGraphics = new HashMap<>();

    private Thread drawThread;

    @Getter
    @Setter
    private Runnable initializationCallback;

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
        managerPresenter.stop();

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

        screenGraphics.clear();
        screenImages.clear();

        windowConfigs.forEach(wc -> {
            ProjectionWindow w = windows.get(wc.getDisplayId());

            if (w != null) {
                GraphicsDevice device = w.getCurrentDevice().getDevice();
                int width = device.getDisplayMode().getWidth();
                int height = device.getDisplayMode().getHeight();

                BufferedImage screenImage = device.getDefaultConfiguration().createCompatibleImage(width, height);
                screenImage.setAccelerationPriority(1.0f);
                Graphics2D screenGraphic = screenImage.createGraphics();

                screenGraphics.put(wc.getDisplayId(), screenGraphic);
                screenImages.put(wc.getDisplayId(), screenImage);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                windowConfigs.forEach(wc -> {
                    ProjectionWindow w = windows.get(wc.getDisplayId());

                    if (w != null) {
                        w.init();
                        w.setCursor(blankCursor);
                        w.setFullScreen(fullScreen);
                    }
                });
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                windowConfigs.forEach(wc -> {
                    ProjectionWindow w = windows.get(wc.getDisplayId());
                    if (w != null) {
                        w.makeVisible();
                    }
                });

                projectionCanvas.init();
                preview.setProjectionCanvas(projectionCanvas);

                managerPresenter.start(windows);

                running = true;
                drawThread = new Thread(WindowManager.this);
                drawThread.start();
                starting = false;
            }
        });
    }

    @Override
    public void run() {
        int frames = 0;
        long timestamp = System.nanoTime();

        Graphics2D targetGraphics = targetRender.createGraphics();

        if (initializationCallback != null) {
            initializationCallback.run();
        }

        while (running) {
            frames++;

            long newTimestamp = System.nanoTime();
            if (newTimestamp - timestamp > 1000000000) {
                // System.out.println(frames);
                frames = 0;
                timestamp = newTimestamp;
            }

            projectionCanvas.paintComponent(targetGraphics);

            windowConfigs.parallelStream().forEach(windowConfig -> {
                Graphics2D g = screenGraphics.get(windowConfig.getDisplayId());

                if (g == null) {
                    return;
                }

                g.setColor(Color.BLACK);
                g.fillRect(windowConfig.getBgFillX(), windowConfig.getBgFillY(), windowConfig.getBgFillWidth(), windowConfig.getBgFillHeight());

                AffineTransform transform = transformAssets.get(windowConfig.getDisplayId());

                g.setTransform(transform);

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g.drawImage(targetRender, 0, 0, null);

                g.setTransform(transform);
                g.translate(windowConfig.getX(), windowConfig.getY());

                BufferedImage bLevelFix = bLevelFixAssets.get(windowConfig.getDisplayId());
                g.drawImage(bLevelFix, windowConfig.getBgFillX(), windowConfig.getBgFillY(), null);

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

            windowConfigs.forEach(windowConfig -> {
                BufferedImage screen = screenImages.get(windowConfig.getDisplayId());
                managerPresenter.update(windowConfig.getDisplayId(), screen);
            });

            Thread.yield();
        }

        windows.values().forEach(ProjectionWindow::shutdown);
        targetGraphics.dispose();
        screenGraphics.forEach((id, g) -> {
            g.dispose();
        });

        drawThread = null;
    }

    @Override
    public int getWidth() {
        int screenStart = windowConfigs.stream().map(WindowConfig::getX).min(Integer::compareTo).orElse(0);
        int screenEnd = windowConfigs.stream().map(wc -> wc.getX() + wc.getWidth()).max(Integer::compareTo).orElse(800);
        return screenEnd - screenStart;
    }

    @Override
    public int getHeight() {
        int screenStart = windowConfigs.stream().map(WindowConfig::getY).min(Integer::compareTo).orElse(0);
        int screenEnd = windowConfigs.stream().map(wc -> wc.getY() + wc.getHeight()).max(Integer::compareTo).orElse(600);
        return screenEnd - screenStart;
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        if (this.fullScreen == fullScreen) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            stopEngine();
            this.fullScreen = fullScreen;
            startEngine();
        });
    }

    public void stop() {
        SwingUtilities.invokeLater(() -> {
            stopEngine();
            projectionCanvas.finish();
        });
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

        devices.forEach(device -> {
            this.windows.put(device.getDevice().getIDstring(), new ProjectionWindow(device));
        });

        startEngine();
    }

    @Override
    public void updateConfigs(List<WindowConfig> windowConfigs) {
        if (running) {
            stopEngine();
            loadWindowConfigs(windowConfigs);
            startEngine();
        } else {
            loadWindowConfigs(windowConfigs);
        }
    }

    private void loadWindowConfigs(List<WindowConfig> windowConfigs) {
        this.windowConfigs = windowConfigs.stream()
                .filter(WindowConfig::isProject)
                .peek(wc -> {
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
                    wc.setProject(device.isProjectionDevice());
                    wc.setDisplayBounds(device.getDevice().getDefaultConfiguration().getBounds());

                    wc.setWidth(device.getDevice().getDisplayMode().getWidth());
                    wc.setHeight(device.getDevice().getDisplayMode().getHeight());
                    wc.setX(0);
                    wc.setY(0);

                    wc.setBgFillX(0);
                    wc.setBgFillY(0);
                    wc.setBgFillWidth(device.getDevice().getDisplayMode().getWidth());
                    wc.setBgFillHeight(device.getDevice().getDisplayMode().getHeight());

                    wc.setBLevelOffset(0);

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
        bLevelFixAssets.clear();

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

            BufferedImage img = new BufferedImage(wc.getBgFillWidth(), wc.getBgFillHeight(), BufferedImage.TYPE_INT_ARGB);
            int bLevelPad = ((Math.round(wc.getBLevelOffset()) & 0xFF) << 24) | 0x00FFFFFF;

            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    img.setRGB(x, y, bLevelPad);
                }
            }

            bLevelFixAssets.put(wc.getDisplayId(), img);
        });

    }
}
