/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.scenes;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import us.guihouse.projector.forms.controllers.projection.BgImageController;
import us.guihouse.projector.forms.controllers.projection.ImageController;
import us.guihouse.projector.forms.controllers.SceneManager;
import us.guihouse.projector.projection.ProjectionManager;

/**
 *
 * @author guilherme
 */
public class BgImageScene {
    public static Parent createScene(ProjectionManager mgr, SceneManager smgr) throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/bg_image.fxml");
        FXMLLoader loader = new FXMLLoader(url);
       
        Parent root = loader.load();
        BgImageController pc = loader.getController();
        
        pc.setSceneManager(smgr);
        pc.initWithProjectionManager(mgr);
        
        return root;
    }
}
