/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

import dev.juhouse.projector.projection2.ProjectionManager;
import dev.juhouse.projector.projection2.WindowManager;
import dev.juhouse.projector.services.SettingsService;
import dev.juhouse.projector.utils.WindowConfigsLoaderProperty;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
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

    public void start() { windowManager.startEngine(); }

    void stop() {
        windowManager.stopEngine();
    }

    public void reloadDevices() {
        windowManager.reloadDevices();
    }

    VBox getMultiPreviewVBox() {
        return windowManager.getPreview();
    }

    WindowConfigsLoaderProperty getWindowConfigsLoaderProperty() {
        return windowManager.getConfigsObserver();
    }
}
