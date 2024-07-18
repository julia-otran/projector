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
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class GraphicsDeviceHelper {
    private final WindowManager windowManager;
    private final ScheduledService<Void> pollEventsService;

    public GraphicsDeviceHelper() {
        SettingsService settingsService = new SettingsService();
        windowManager = new WindowManager(settingsService);



        pollEventsService = new ScheduledService<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        Platform.runLater(() -> {
                            windowManager.getBridge().runOnMainThreadLoop();
                        });
                        return null;
                    }
                };
            }
        };
    }

    ProjectionManager getProjectionManager() {
        return windowManager.getManager();
    }

    public void start() {
        windowManager.startEngine();
        pollEventsService.setPeriod(Duration.millis(200));
        pollEventsService.start();
    }

    void stop() {
        pollEventsService.cancel();
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
