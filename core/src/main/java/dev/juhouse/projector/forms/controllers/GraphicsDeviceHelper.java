/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

import dev.juhouse.projector.other.GraphicsFinder;
import dev.juhouse.projector.projection.ProjectionManager;
import dev.juhouse.projector.projection.WindowManager;
import dev.juhouse.projector.services.SettingsService;
import dev.juhouse.projector.utils.WindowConfigsLoader;
import javafx.scene.image.ImageView;

/**
 *
 * @author guilherme
 */
public class GraphicsDeviceHelper {
    private final WindowManager windowManager;

    public GraphicsDeviceHelper() {
        SettingsService settingsService = new SettingsService();
        windowManager = new WindowManager(settingsService);
    }

    ProjectionManager getProjectionManager() {
        return windowManager.getManager();
    }

    public void setInitCallback(Runnable runnable) {
        windowManager.setInitializationCallback(runnable);
    }

    public void init() {
        reloadDevices();
    }

    void stop() {
        windowManager.stop();
    }

    public void reloadDevices() {
        GraphicsFinder.Device defaulDevice = GraphicsFinder.getDefaultDevice();
        windowManager.setDefaultDevice(defaulDevice.getDevice());
        windowManager.setDevices(GraphicsFinder.getAvailableDevices());
    }

    ImageView getPreviewPanel() {
        return windowManager.getPreviewPanel();
    }

    WindowConfigsLoader getWindowConfigsLoader() {
        return windowManager.getWindowConfigsLoader();
    }
}
