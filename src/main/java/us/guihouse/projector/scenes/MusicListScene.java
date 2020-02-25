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
import javafx.stage.Stage;
import us.guihouse.projector.forms.controllers.AddMusicCallback;
import us.guihouse.projector.forms.controllers.projection.ImageController;
import us.guihouse.projector.forms.controllers.MusicListController;
import us.guihouse.projector.services.ManageMusicService;

/**
 *
 * @author guilherme
 */
public class MusicListScene {
    public static Parent createMusicListScene(AddMusicCallback callback,  ManageMusicService manageMusicService, Stage listMusicStage) throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/music_list.fxml");
        FXMLLoader loader = new FXMLLoader(url);
       
        Parent root = loader.load();
        MusicListController ctrl = loader.getController();
        ctrl.setAddMusicCallback(callback);
        ctrl.setManageMusicService(manageMusicService);
        ctrl.setCurrentStage(listMusicStage);
        
        return root;
    }
}
