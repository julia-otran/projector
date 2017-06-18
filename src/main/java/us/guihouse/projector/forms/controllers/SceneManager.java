/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author guilherme
 */
public interface SceneManager {
    void goToScene(Scene scene);
    void goToWorkspace();
    Stage getStage();
}
