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
import dev.juhouse.projector.forms.controllers.projection.BrowserController;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class BrowserSubScene extends ProjectionItemSubScene {

    public static BrowserSubScene createScene(double width, double height) throws IOException {
        URL url = BrowserSubScene.class.getClassLoader().getResource("fxml/browser.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();
        BrowserSubScene scene = new BrowserSubScene(root, width, height);
        scene.setController(loader.getController());
        return scene;
    }

    private BrowserSubScene(Parent root, double width, double height) {
        super(root, width, height);
    }

    @Override
    public BrowserController getController() {
        return (BrowserController) super.getController();
    }

    private void setController(BrowserController controller) {
        super.setController(controller);
    }
}
