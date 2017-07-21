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
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import us.guihouse.projector.other.GraphicsFinder;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.ProjectionWindow;
import us.guihouse.projector.services.SettingsService;

/**
 *
 * @author guilherme
 */
public class GraphicsDeviceHelper {

    private final Menu projectionScreenMenu;
    private MenuItem reloadItem;
    private ProjectionWindow projectionWindow;
    private final SettingsService settingsService = new SettingsService();

    public GraphicsDeviceHelper(Menu projectionScreenMenu) {
        this.projectionScreenMenu = projectionScreenMenu;
        buildProjectionFrame();
        buildReloadItem();
        reloadDevices();
    }

    public ProjectionManager getProjectionManager() {
        return projectionWindow.getManager();
    }

    void stop() {
        projectionWindow.stop();
        projectionWindow.stopPreview();
    }

    void updatePreviewSize(int x, int y, int maxWidth, int maxHeight) {
        projectionWindow.updatePreviewSize(x, y, maxWidth, maxHeight);
    }

    private void reloadDevices() {
        projectionScreenMenu.getItems().clear();
        projectionScreenMenu.getItems().add(reloadItem);

        ToggleGroup group = new ToggleGroup();

        GraphicsFinder.getAvailableDevices().stream().map((GraphicsFinder.Device dev) -> {
            return buildItem(dev);
        }).forEachOrdered((item) -> {
            item.setToggleGroup(group);

            if (item.isSelected()) {
                item.fire();
            }

            projectionScreenMenu.getItems().add(item);
        });
    }

    private void changeDevice(final GraphicsFinder.Device device) {
        projectionWindow.setDevice(device.getDevice());
    }

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

    private RadioMenuItem buildItem(final GraphicsFinder.Device dev) {
        RadioMenuItem item = new RadioMenuItem();
        item.setText(dev.getName());

        if (dev.isProjectionDevice()) {
            item.setSelected(true);
        } else {
            item.setSelected(false);
        }

        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                changeDevice(dev);
            }
        });

        return item;
    }

    private void buildProjectionFrame() {
        projectionWindow = new ProjectionWindow(settingsService);
    }
}
