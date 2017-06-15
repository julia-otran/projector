/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector;

import us.guihouse.projector.forms.MainFrame;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import us.guihouse.projector.projection.ProjectionFrame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author 15096134
 */
public class Projector extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL url = getClass().getClassLoader().getResource("fxml/workspace.fxml");
        Parent root = FXMLLoader.load(url);
        
        Scene scene = new Scene(root, 800, 600);
        
        primaryStage.setTitle("Projector");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
