package dev.juhouse.projector.utils;

import dev.juhouse.projector.other.ProjectorPreferences;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.io.*;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class WindowConfigsObserver implements Runnable, WindowConfigsLoaderProperty {

    private Thread thread;
    private final WindowConfigsObserverCallback callback;

    @Getter
    private final ObservableList<String> configFiles = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<String> loadedConfigFile = new ReadOnlyObjectWrapper<>();

    public interface WindowConfigsObserverCallback {
        void createConfigs(String filePath);
        void updateConfigs(String filePath);
    }

    public WindowConfigsObserver(WindowConfigsObserverCallback callback) {
        this.callback = callback;
    }

    public ReadOnlyProperty<String> loadedConfigFileProperty() {
        return loadedConfigFile.getReadOnlyProperty();
    }

    public void start() {
        File configsPath = FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.toFile();

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

        String[] files = FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.toFile().list();
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
            FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.register(watcher,
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

                Path child = FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.resolve(filename);

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

        File configFile = FileSystems.getDefault().getPath(FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.toString(), fileName).toFile();

        if (!configFile.canRead()) {
            return false;
        }

        return loadConfigFile(configFile);
    }

    public void loadConfigs(String fileName) {
        File configFile = FileSystems.getDefault().getPath(FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.toString(), fileName).toFile();

        if (configFile.canRead()) {
            loadConfigFile(configFile);
        }
    }

    public void loadDefaultConfigs() {
        ProjectorPreferences.setWindowConfigFile(null);
        callback.updateConfigs(null);
        Platform.runLater(() -> loadedConfigFile.set(null));
    }

    @Override
    public boolean createConfigFileFromDefaults(String name) {
        File configFile = FileSystems.getDefault().getPath(FilePaths.PROJECTOR_WINDOW_CONFIG_PATH.toString(), name).toFile();

        try {
            if (configFile.createNewFile()) {
                ProjectorPreferences.setWindowConfigFile(configFile.getName());
                callback.createConfigs(configFile.toString());
                return true;
            }
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    private boolean loadConfigFile(File configFile) {
        if (configFile != null && configFile.canRead()) {
            ProjectorPreferences.setWindowConfigFile(configFile.getName());
            Platform.runLater(() -> loadedConfigFile.set(configFile.getName()));
            callback.updateConfigs(configFile.toString());
            return true;
        }

        return false;
    }
}
