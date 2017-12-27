/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.scenes;

import java.util.UUID;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import us.guihouse.projector.forms.controllers.ControllerObserver;
import us.guihouse.projector.forms.controllers.ProjectionController;
import us.guihouse.projector.forms.controllers.SceneManager;
import us.guihouse.projector.other.Identifiable;
import us.guihouse.projector.projection.ProjectionManager;

/**
 *
 * @author guilherme
 */
public abstract class ProjectionItemSubScene extends SubScene implements ControllerObserver, Identifiable {

    private final String identity;
    private ProjectionController controller;
    private SceneObserver observer;
    private String title;

    protected ProjectionItemSubScene(Parent root, String title, double width, double height) {
        super(root, width, height);
        this.title = title;
        this.identity = UUID.randomUUID().toString();
    }

    public SceneObserver getObserver() {
        return observer;
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
        controller.initWithProjectionManager(projectionManager);
    }

    @Override
    public void onTitleChanged(String newTitle) {
        this.title = newTitle;

        if (observer != null) {
            observer.titleChanged();
        }
    }

    public String getTitle() {
        return title;
    }

    public String toString() {
        return title;
    }
    
    public void onEscapeKeyPressed() {
        if (controller != null) {
            controller.onEscapeKeyPressed();
        }
    }

    @Override
    public String getIdentity() {
        return identity;
    }
}
