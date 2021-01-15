package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static java.awt.image.BufferedImage.TYPE_INT_BGR;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class WindowManagerPresenter{
    private int frames = 0;
    private long timestamp = 0;

    private boolean running;
    private Thread thread;
    private final HashMap<String, BufferedImage> outputs = new HashMap<>();

    private HashMap<String, ProjectionWindow> windows;

    public void start(HashMap<String, ProjectionWindow> windows) {
        this.windows = windows;

        outputs.clear();
        windows.forEach((id, w) -> {
                GraphicsDevice dev = w.getCurrentDevice().getDevice();
                Rectangle bounds = dev.getDefaultConfiguration().getBounds();

                BufferedImage img = new BufferedImage(bounds.width, bounds.height, TYPE_INT_BGR);
                outputs.put(id, img);
        });
    }

    public void stop() {
    }

    public void update(String id, BufferedImage src) {
        windows.get(id).updateOutput(src);
    }
}
