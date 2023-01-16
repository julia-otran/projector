/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.scenes;

import java.io.IOException;
import java.net.URL;

import dev.juhouse.projector.projection.ProjectionManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import dev.juhouse.projector.forms.controllers.projection.BgImageController;
import dev.juhouse.projector.forms.controllers.projection.ImageController;
import dev.juhouse.projector.forms.controllers.SceneManager;

/**
 *
 * @author guilherme
 */
public class BgImageScene {
    public static Parent createScene(ProjectionManager mgr, SceneManager sceneManager) throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/bg_image.fxml");
        FXMLLoader loader = new FXMLLoader(url);
       
        Parent root = loader.load();
        BgImageController pc = loader.getController();
        
        pc.setSceneManager(sceneManager);
        pc.initWithProjectionManager(mgr);
        
        return root;
    }
}
