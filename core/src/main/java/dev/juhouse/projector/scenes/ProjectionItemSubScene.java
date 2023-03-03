/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.scenes;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import dev.juhouse.projector.projection2.ProjectionManager;
import dev.juhouse.projector.repositories.ProjectionListRepository;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import lombok.Getter;
import lombok.Setter;
import dev.juhouse.projector.forms.controllers.ControllerObserver;
import dev.juhouse.projector.forms.controllers.projection.ProjectionController;
import dev.juhouse.projector.forms.controllers.SceneManager;
import dev.juhouse.projector.models.ProjectionListItem;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public abstract class ProjectionItemSubScene extends SubScene implements ControllerObserver {

    private ProjectionController controller;
    private SceneObserver observer;

    private Map<String, String> itemProperties;

    @Getter
    @Setter
    private ProjectionListRepository projectionListRepository;

    @Getter
    @Setter
    private ProjectionListItem projectionListItem;

    protected ProjectionItemSubScene(Parent root,
                                     double width,
                                     double height) {
        super(root, width, height);
    }

    public void setObserver(SceneObserver observer) {
        this.observer = observer;
    }

    public void setSceneManager(SceneManager sceneManager) {
        controller.setSceneManager(sceneManager);
    }

    public ProjectionController getController() {
        return controller;
    }

    protected void setController(ProjectionController controller) {
        this.controller = controller;
        controller.setObserver(this);
    }

    public void initWithProjectionManager(ProjectionManager projectionManager) {
        try {
            this.itemProperties = projectionListRepository.getItemProperties(projectionListItem.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            this.itemProperties = new HashMap<>();
        }

        controller.initWithProjectionManager(projectionManager);
    }

    @Override
    public void onTitleChanged(String newTitle) {
        try {
            projectionListRepository.updateItemTitle(projectionListItem, newTitle);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (observer != null) {
            observer.titleChanged(projectionListItem);
        }
    }
    
    public void onEscapeKeyPressed() {
        if (controller != null) {
            controller.onEscapeKeyPressed();
        }
    }

    public void stop() {
        if (controller != null) {
            controller.stop();
        }
    }

    @Override
    public void updateProperty(String key, String value) {
        itemProperties.remove(key);
        itemProperties.put(key, value);

        try {
            projectionListRepository.updateItemProperties(projectionListItem.getId(), itemProperties);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getProperty(String key) {
        return itemProperties.get(key);
    }
}
