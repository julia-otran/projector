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

import static java.nio.file.StandardWatchEventKinds.*;
import static us.guihouse.projector.utils.FilePaths.*;

public class WindowConfigsLoader implements Runnable {

    private Thread thread;
    private final WindowConfigsObserver observer;
    private final Gson gson = new Gson();

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
        WatchService watcher = null;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            PROJECTOR_DATA_PATH.register(watcher,
                    ENTRY_CREATE,
                    ENTRY_DELETE,
                    ENTRY_MODIFY);
        } catch (IOException x) {
            x.printStackTrace();
            return;
        }

        for (;;) {
            // wait for key to be signaled
            WatchKey key;

            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // This key is registered only
                // for ENTRY_CREATE events,
                // but an OVERFLOW event can
                // occur regardless if events
                // are lost or discarded.
                if (kind == OVERFLOW) {
                    continue;
                }

                // The filename is the
                // context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path filename = ev.context();

                Path child = PROJECTOR_DATA_PATH.resolve(filename);

                if (child.endsWith(PROJECTOR_WINDOW_CONFIG_FILE_NAME)) {
                    loadFile();
                }
            }

            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
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
        File settingsFolder = PROJECTOR_DATA_PATH.toFile();

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
