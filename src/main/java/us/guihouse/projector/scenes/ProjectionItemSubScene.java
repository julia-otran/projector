/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.scenes;

import javafx.scene.Parent;
import javafx.scene.SubScene;
import us.guihouse.projector.forms.controllers.ControllerObserver;
import us.guihouse.projector.forms.controllers.ProjectionController;
import us.guihouse.projector.projection.ProjectionManager;

/**
 *
 * @author guilherme
 */
public abstract class ProjectionItemSubScene extends SubScene implements ControllerObserver {
    private ProjectionController controller;
    private SceneObserver observer;
    
    protected ProjectionItemSubScene(Parent root, double width, double height) {
        super(root, width, height);
    }

    public SceneObserver getObserver() {
        return observer;
    }

    public void setObserver(SceneObserver observer) {
        this.observer = observer;
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
        if (observer != null) {
            observer.titleChanged(this, newTitle);
        }
    }
}
