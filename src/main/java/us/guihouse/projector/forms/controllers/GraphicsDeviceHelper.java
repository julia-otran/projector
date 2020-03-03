/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.WindowManager;
import us.guihouse.projector.services.SettingsService;
import us.guihouse.projector.utils.WindowConfigsLoader;

/**
 *
 * @author guilherme
 */
public class GraphicsDeviceHelper {
    private WindowManager windowManager;
    private final SettingsService settingsService = new SettingsService();

    public GraphicsDeviceHelper() {
        windowManager = new WindowManager(settingsService);
    }

    ProjectionManager getProjectionManager() {
        return windowManager.getManager();
    }

    public Runnable getInitCallback() {
        return windowManager.getInitializationCallback();
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

    JPanel getPreviewPanel() {
        return windowManager.getPreviewPanel();
    }

    WindowConfigsLoader getWindowConfigsLoader() {
        return windowManager.getWindowConfigsLoader();
    }
}
