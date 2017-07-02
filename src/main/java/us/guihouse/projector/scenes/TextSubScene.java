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

/**
 *
 * @author guilherme
 */
public class TextSubScene extends ProjectionItemSubScene {

    public static ProjectionItemSubScene createScene(double width, double height) throws IOException {
        URL url = TextSubScene.class.getClassLoader().getResource("fxml/text.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();
        TextSubScene scene = new TextSubScene(root, width, height);
        scene.setController(loader.getController());
        return scene;
    }

    private TextSubScene(Parent root, double width, double height) {
        super(root, "Novo Texto", width, height);
    }
}
