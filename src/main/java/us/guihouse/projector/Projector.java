/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector;

import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import us.guihouse.projector.forms.controllers.SceneManager;
import us.guihouse.projector.forms.controllers.WorkspaceController;

/**
 *
 * @author 15096134
 */
public class Projector extends Application {
    private static WorkspaceController controller;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        launch(args);
        controller.dispose();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL url = getClass().getClassLoader().getResource("fxml/workspace.fxml");
        
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Scene workspaceScene = new Scene(root, 800, 600);
        
        controller = loader.getController();
        controller.setSceneManager(new SceneManager() {
            @Override
            public void goToScene(Scene scene) {
                primaryStage.setScene(scene);
            }

            @Override
            public void goToWorkspace() {
                primaryStage.setScene(workspaceScene);
            }

            @Override
            public Stage getStage() {
                return primaryStage;
            }
        });
        
        primaryStage.setTitle("Projector");
        primaryStage.setScene(workspaceScene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        controller.stop();
        super.stop();
    }
}
