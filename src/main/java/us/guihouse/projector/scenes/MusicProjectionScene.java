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
import us.guihouse.projector.forms.controllers.MusicProjectionController;
import us.guihouse.projector.forms.controllers.ProjectionController;

/**
 *
 * @author guilherme
 */
public class MusicProjectionScene extends ProjectionItemSubScene {
    
    public static MusicProjectionScene createScene(double width, double height) throws IOException {
        URL url = MusicProjectionScene.class.getClassLoader().getResource("fxml/music_projection.fxml");
        FXMLLoader loader = new FXMLLoader(url);
        
        Parent root = loader.load();
        MusicProjectionScene scene = new MusicProjectionScene(root, width, height);
        scene.setController(loader.getController());
        return scene;
    }
    
    private MusicProjectionScene(Parent root, double width, double height) {
        super(root, width, height);
    }

    @Override
    public MusicProjectionController getController() {
        return (MusicProjectionController) super.getController(); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void setMusicId(Integer musicId) {
        getController().setMusicId(musicId);
    }
}
