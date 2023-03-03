/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.scenes;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import dev.juhouse.projector.dtos.ImportingMusicDTO;
import dev.juhouse.projector.forms.controllers.BackCallback;
import dev.juhouse.projector.forms.controllers.projection.ImageController;
import dev.juhouse.projector.forms.controllers.MusicFormController;
import dev.juhouse.projector.services.ManageMusicService;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class MusicFormScene {
    public static Parent createMusicFormScene(ManageMusicService manageMusicService, BackCallback callback) throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/music_form.fxml");
        FXMLLoader loader = new FXMLLoader(url);
       
        Parent root = loader.load();
        
        MusicFormController ctrl = loader.getController();
        ctrl.setManageMusicService(manageMusicService);
        ctrl.setBackCallback(callback);
        ctrl.init();
        
        
        return root;
    }
    
    public static Parent editMusicFormScene(ManageMusicService manageMusicService, BackCallback callback, Integer id) throws IOException, ManageMusicService.PersistenceException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/music_form.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();

        MusicFormController ctrl = loader.getController();
        ctrl.setManageMusicService(manageMusicService);
        ctrl.setBackCallback(callback);
        ctrl.init(id);

        return root;
    }

    public static Parent createMusicFormScene(ManageMusicService manageMusicService, BackCallback callback, ImportingMusicDTO music) throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/music_form.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();

        MusicFormController ctrl = loader.getController();
        ctrl.setManageMusicService(manageMusicService);
        ctrl.setBackCallback(callback);
        ctrl.init(music);

        return root;
    }
}
