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
import dev.juhouse.projector.forms.controllers.projection.PlayerController;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class PlayerSubScene extends ProjectionItemSubScene {

    public static PlayerSubScene createScene(double width, double height) throws IOException {
        URL url = PlayerSubScene.class.getClassLoader().getResource("fxml/player.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();
        PlayerSubScene scene = new PlayerSubScene(root, width, height);
        scene.setController(loader.getController());

        scene.visibleProperty().set(false);
        scene.visibleProperty().addListener((prop, oldVal, newVal) -> scene.getController().setVisible(newVal));

        return scene;
    }

    private PlayerSubScene(Parent root, double width, double height) {
        super(root, width, height);
    }

    @Override
    public PlayerController getController() {
        return (PlayerController) super.getController();
    }

    private void setController(PlayerController controller) {
        super.setController(controller);
    }
}
