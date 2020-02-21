/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleGroup;
import javax.swing.JPanel;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.projection.WindowManager;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.services.SettingsService;

import java.util.stream.Collectors;

/**
 *
 * @author guilherme
 */
public class GraphicsDeviceHelper {

    private final Menu projectionScreenMenu;
    private MenuItem reloadItem;
    private WindowManager windowManager;
    private final SettingsService settingsService = new SettingsService();

    GraphicsDeviceHelper(Menu projectionScreenMenu) {
        this.projectionScreenMenu = projectionScreenMenu;
        buildProjectionFrame();
        buildReloadItem();
        reloadDevices();
    }

    ProjectionManager getProjectionManager() {
        return windowManager.getManager();
    }

    void stop() {
        windowManager.stop();
    }

    private void reloadDevices() {
        projectionScreenMenu.getItems().clear();
        projectionScreenMenu.getItems().add(reloadItem);

        ToggleGroup group = new ToggleGroup();

        GraphicsFinder.Device defaulDevice = GraphicsFinder.getDefaultDevice();
        windowManager.setDefaultDevice(defaulDevice.getDevice());

        windowManager.setDevices(GraphicsFinder
                .getAvailableDevices()
                .stream()
                .filter(GraphicsFinder.Device::isProjectionDevice)
                .collect(Collectors.toList()));
    }

//    private void changeDevice(final GraphicsFinder.Device device) {
//        projectionWindow.setDevice(device.getDevice());
//    }
    private void buildReloadItem() {
        reloadItem = new MenuItem();
        reloadItem.setText("Redetectar Telas");
        reloadItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                reloadDevices();
            }
        });
    }

//    private RadioMenuItem buildItem(final GraphicsFinder.Device dev) {
//        RadioMenuItem item = new RadioMenuItem();
//        item.setText(dev.getName());
//
//        if (dev.isProjectionDevice()) {
//            item.setSelected(true);
//        } else {
//            item.setSelected(false);
//        }
//
//        item.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                changeDevice(dev);
//            }
//        });
//
//        return item;
//    }
    private void buildProjectionFrame() {
        windowManager = new WindowManager(settingsService);
    }

    JPanel getPreviewPanel() {
        return windowManager.getPreviewPanel();
    }
}
