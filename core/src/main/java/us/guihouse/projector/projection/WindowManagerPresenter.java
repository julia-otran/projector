package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class WindowManagerPresenter implements Runnable {
    private boolean running;
    private Thread thread;
    private final HashMap<String, BufferedImage> outputs = new HashMap<>();

    private HashMap<String, ProjectionWindow> windows;

    public void start(HashMap<String, ProjectionWindow> windows) {
        this.windows = windows;

        outputs.clear();
        windows.forEach((id, w) -> {
            if (w.getFrame() != null) {
                GraphicsDevice dev = w.getCurrentDevice().getDevice();

                BufferedImage img = dev.getDefaultConfiguration().createCompatibleImage(dev.getDisplayMode().getWidth(), dev.getDisplayMode().getHeight());
                outputs.put(id, img);
            }
        });

        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;

        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            thread = null;
        }
    }

    public void update(String id, BufferedImage src) {
        ProjectionWindow w = windows.get(id);
        BufferedImage dst = outputs.get(id);

        if (w != null) {
            src.copyData(dst.getRaster());
        }
    }

    @Override
    public void run() {
        while (running) {
            outputs.forEach((id, img) -> {
                windows.get(id).updateOutput(outputs.get(id));
            });

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
