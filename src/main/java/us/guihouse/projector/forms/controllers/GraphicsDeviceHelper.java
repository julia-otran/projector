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
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.WindowManager;
import us.guihouse.projector.services.SettingsService;

/**
 *
 * @author guilherme
 */
public class GraphicsDeviceHelper {

    private WindowManager windowManager;
    private final SettingsService settingsService = new SettingsService();

    public GraphicsDeviceHelper() {
        buildProjectionFrame();
        reloadDevices();
    }

    ProjectionManager getProjectionManager() {
        return windowManager.getManager();
    }

    void stop() {
        windowManager.stop();
    }

    public void reloadDevices() {
        GraphicsFinder.Device defaulDevice = GraphicsFinder.getDefaultDevice();
        windowManager.setDefaultDevice(defaulDevice.getDevice());

        windowManager.setDevices(GraphicsFinder
                .getAvailableDevices()
                .stream()
                .filter(GraphicsFinder.Device::isProjectionDevice)
                .collect(Collectors.toList()));
    }

    private void buildProjectionFrame() {
        windowManager = new WindowManager(settingsService);
    }

    JPanel getPreviewPanel() {
        return windowManager.getPreviewPanel();
    }
}
