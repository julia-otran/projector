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
import us.guihouse.projector.forms.controllers.ImageController;

/**
 *
 * @author guilherme
 */
public class MusicListScene {
    public static Parent createMusicListScene() throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/music_list.fxml");
        FXMLLoader loader = new FXMLLoader(url);
       
        Parent root = loader.load();
        
        return root;
    }
}
