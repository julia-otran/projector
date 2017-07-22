/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import javafx.fxml.Initializable;
import us.guihouse.projector.projection.ProjectionManager;

/**
 *
 * @author guilherme
 */
public abstract class ProjectionController implements Initializable {
    private ControllerObserver observer;
    ProjectionManager projectionManager;

    public ControllerObserver getObserver() {
        return observer;
    }

    public void setObserver(ControllerObserver observer) {
        this.observer = observer;
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
}
