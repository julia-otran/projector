package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class WindowManagerPresenter implements Runnable {
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
            if (w.getFrame() != null) {
                GraphicsDevice dev = w.getCurrentDevice().getDevice();

                BufferedImage img = dev.getDefaultConfiguration().createCompatibleImage(dev.getDefaultConfiguration().getBounds().width, dev.getDefaultConfiguration().getBounds().height);
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
        BufferedImage dst = outputs.get(id);

        if (dst != null) {
            src.copyData(dst.getRaster());
        }
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        while (running) {
            frames++;

            long newTimestamp = System.nanoTime();
            if (newTimestamp - timestamp > 1000000000) {
                // System.out.println(windows.entrySet().stream().findFirst().map(Map.Entry::getKey).orElse("") + " - " + frames);
                frames = 0;
                timestamp = newTimestamp;
            }

            outputs.forEach((id, img) -> windows.get(id).updateOutput(img));

            //Thread.yield();

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
