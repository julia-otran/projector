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
import dev.juhouse.projector.forms.controllers.projection.ImageController;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ImageSubScene extends ProjectionItemSubScene {

    public static ProjectionItemSubScene createScene(double width, double height) throws IOException {
        URL url = ImageController.class.getClassLoader().getResource("fxml/image.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();
        ImageSubScene scene = new ImageSubScene(root, width, height);
        scene.setController(loader.getController());
        return scene;
    }

    private ImageSubScene(Parent root, double width, double height) {
        super(root, width, height);
    }
}
