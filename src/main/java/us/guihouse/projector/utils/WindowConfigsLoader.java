package us.guihouse.projector.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.models.WindowConfigBlend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

public class WindowConfigsLoader implements Runnable {

    private Thread thread;
    private final WindowConfigsObserver observer;
    private final Gson gson = new Gson();


    private static final Path PROJECTOR_SETTINGS_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector");
    private static final Path PROJECTOR_WINDOW_CONFIG_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", "window-configs.json");
    private static final File PROJECTOR_WINDOW_CONFIG_FILE = PROJECTOR_WINDOW_CONFIG_PATH.toFile();

    public interface WindowConfigsObserver {
        void updateConfigs(List<WindowConfig> windowConfigs);
        List<WindowConfig> getDefaultConfigs();
    }

    public WindowConfigsLoader(WindowConfigsObserver observer) {
        this.observer = observer;
    }

    public void start() {
        if (!PROJECTOR_WINDOW_CONFIG_FILE.exists()) {
            if (!createDefaultFile()) {
                observer.updateConfigs(observer.getDefaultConfigs());
            }
        }

        loadFile();

        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }

        thread = null;
    }

    public void run() {
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            final WatchKey watchKey = PROJECTOR_SETTINGS_PATH.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                final WatchKey wk = watchService.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    //we only register "ENTRY_MODIFY" so the context is always a Path.
                    final Path changed = (Path) event.context();
                    if (changed.endsWith("myFile.txt")) {
                        loadFile();
                    }
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadFile() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(PROJECTOR_WINDOW_CONFIG_FILE), StandardCharsets.UTF_8));

            List<WindowConfig> configs = gson.fromJson(in, new TypeToken<List<WindowConfig>>(){}.getType());

            int blendId = 0;

            for (WindowConfig wc : configs) {
                for (WindowConfigBlend b : wc.getBlends()) {
                    b.setId(blendId);
                    blendId++;
                }
            }

            observer.updateConfigs(configs);
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private boolean createDefaultFile() {
        File settingsFolder = PROJECTOR_SETTINGS_PATH.toFile();

        if (!settingsFolder.exists()) {
            if (!settingsFolder.mkdir()) {
                return false;
            }
        }

        try {
            if (!PROJECTOR_WINDOW_CONFIG_FILE.createNewFile()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
        String json = gsonBuilder.toJson(observer.getDefaultConfigs());

        try {
            OutputStreamWriter writer =
                    new OutputStreamWriter(new FileOutputStream(PROJECTOR_WINDOW_CONFIG_FILE), StandardCharsets.UTF_8);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
