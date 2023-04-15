/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

import javafx.application.HostServices;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface SceneManager {
    void goToParent(Parent toor);
    void goToWorkspace();
    Stage getStage();
    HostServices getHostServices();
}
