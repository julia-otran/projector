/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers.projection;

import dev.juhouse.projector.projection2.ProjectionManager;
import javafx.fxml.Initializable;
import dev.juhouse.projector.forms.controllers.ControllerObserver;
import dev.juhouse.projector.forms.controllers.SceneManager;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public abstract class ProjectionController implements Initializable {
    private ControllerObserver observer;
    ProjectionManager projectionManager;
    private SceneManager sceneManager;

    public ControllerObserver getObserver() {
        return observer;
    }

    public void setObserver(ControllerObserver observer) {
        this.observer = observer;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void setSceneManager(SceneManager manager) {
        this.sceneManager = manager;
    }
    
    protected void notifyTitleChange(String title) {
        if (observer != null) {
            observer.onTitleChanged(title);
        }
    }

    public ProjectionManager getProjectionManager() {
        return projectionManager;
    }
    
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        this.projectionManager = projectionManager;
    }
    
    public abstract void onEscapeKeyPressed();

    public abstract void stop();
}
