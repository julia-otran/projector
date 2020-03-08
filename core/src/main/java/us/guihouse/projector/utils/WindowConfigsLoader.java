package us.guihouse.projector.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.models.WindowConfigBlend;
import us.guihouse.projector.other.ProjectorPreferences;

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

    @Getter
    private final ObservableList<String> configFiles = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<String> loadedConfigFile = new ReadOnlyObjectWrapper<>();

    public interface WindowConfigsObserver {
        void updateConfigs(List<WindowConfig> windowConfigs);
        List<WindowConfig> getDefaultConfigs();
    }

    public WindowConfigsLoader(WindowConfigsObserver observer) {
        this.observer = observer;
    }

    public ReadOnlyProperty<String> loadedConfigFileProperty() {
        return loadedConfigFile.getReadOnlyProperty();
    }

    public void start() {
        File configsPath = PROJECTOR_WINDOW_CONFIG_PATH.toFile();

        if (configsPath.isDirectory() || configsPath.mkdirs()) {
            loadConfigFiles();

            if (!loadSavedConfigs()) {
                loadDefaultConfigs();
            }

            thread = new Thread(this);
            thread.start();
        } else {
            loadDefaultConfigs();
        }
    }

    private void loadConfigFiles() {
        configFiles.clear();

        String[] files = PROJECTOR_WINDOW_CONFIG_PATH.toFile().list();
        if (files != null) {
            configFiles.addAll(files);
        }
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }

        thread = null;
    }

    public void run() {
        WatchService watcher;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            PROJECTOR_WINDOW_CONFIG_PATH.register(watcher,
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

                Platform.runLater(this::loadConfigFiles);

                // The filename is the
                // context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path filename = ev.context();

                Path child = PROJECTOR_WINDOW_CONFIG_PATH.resolve(filename);

                if (ProjectorPreferences.getWindowConfigFile() != null && child.endsWith(ProjectorPreferences.getWindowConfigFile())) {
                    loadSavedConfigs();
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

    private boolean loadSavedConfigs() {
        String fileName = ProjectorPreferences.getWindowConfigFile();

        if (fileName == null) {
            return false;
        }

        File configFile = FileSystems.getDefault().getPath(PROJECTOR_WINDOW_CONFIG_PATH.toString(), fileName).toFile();

        if (!configFile.canRead()) {
            return false;
        }

        return loadConfigs(configFile);
    }

    public void loadConfigs(String fileName) {
        File configFile = FileSystems.getDefault().getPath(PROJECTOR_WINDOW_CONFIG_PATH.toString(), fileName).toFile();

        if (configFile.canRead()) {
            loadConfigs(configFile);
        }
    }

    public void loadDefaultConfigs() {
        ProjectorPreferences.setWindowConfigFile(null);
        observer.updateConfigs(observer.getDefaultConfigs());
        Platform.runLater(() -> {
            loadedConfigFile.set(null);
        });
    }

    private boolean loadConfigs(File configFile) {
        if (configFile != null && configFile.canRead()) {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(configFile), StandardCharsets.UTF_8));

                List<WindowConfig> configs = gson.fromJson(in, new TypeToken<List<WindowConfig>>() {}.getType());

                ProjectorPreferences.setWindowConfigFile(configFile.getName());

                Platform.runLater(() -> {
                    loadedConfigFile.set(configFile.getName());
                });

                int blendId = 0;

                for (WindowConfig wc : configs) {
                    for (WindowConfigBlend b : wc.getBlends()) {
                        b.setId(blendId);
                        blendId++;
                    }
                }

                observer.updateConfigs(configs);

                return true;
            } catch (JsonIOException | JsonSyntaxException | FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        return false;
    }

    public boolean createConfigFileFromDefaults(String name) {
        File file = FileSystems.getDefault().getPath(PROJECTOR_WINDOW_CONFIG_PATH.toString(), name).toFile();

        try {
            if (file.exists() || !file.createNewFile()) {
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
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        loadConfigs(file);
        return true;
    }
}
