/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector;

import com.mashape.unirest.http.Unirest;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import us.guihouse.projector.forms.controllers.GraphicsDeviceHelper;
import us.guihouse.projector.forms.controllers.SceneManager;
import us.guihouse.projector.forms.controllers.WorkspaceController;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import us.guihouse.projector.utils.ThemeFinder;
import us.guihouse.projector.utils.VlcPlayerFactory;

import javax.swing.*;

/**
 *
 * @author 15096134
 */
public class Projector extends Application implements Runnable {
    private WorkspaceController controller;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "True");
        launch(args);
    }

    @Override
    public void run() {
        ThemeFinder.loadThemes();

        VlcPlayerFactory.init();

        final GraphicsDeviceHelper graphicsHelper = new GraphicsDeviceHelper();

        graphicsHelper.setInitCallback(() -> Platform.runLater(() -> {
            graphicsHelper.setInitCallback(null);
            controller.init(graphicsHelper);
        }));

        graphicsHelper.init();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SQLiteJDBCDriverConnection.connect();
        SQLiteJDBCDriverConnection.migrate();

        Platform.runLater(() -> Thread.currentThread().setPriority(Thread.MIN_PRIORITY));
        SwingUtilities.invokeLater(() -> Thread.currentThread().setPriority(Thread.MIN_PRIORITY));

        primaryStage.setTitle("Projector");
        primaryStage.setMaxWidth(Double.MAX_VALUE);
        primaryStage.setMaxHeight(Double.MAX_VALUE);
        
        URL url = getClass().getClassLoader().getResource("fxml/workspace.fxml");
        
        FXMLLoader loader = new FXMLLoader(url);
        
        Parent workspaceRoot = loader.load();
        controller = loader.getController();
        
        Scene workspaceScene = new Scene(workspaceRoot, 1000, 700);
        
        controller.setSceneManager(new SceneManager() {
            @Override
            public void goToParent(Parent scene) {
                workspaceScene.setRoot(scene);
            }

            @Override
            public void goToWorkspace() {
                workspaceScene.setRoot(workspaceRoot);
            }

            @Override
            public Stage getStage() {
                return primaryStage;
            }

        });
        
        workspaceScene.addEventFilter(KeyEvent.KEY_PRESSED, t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                controller.onEscapeKeyPressed();
            }
        });
        
        primaryStage.setScene(workspaceScene);

        primaryStage.show();
        
        primaryStage.setOnCloseRequest(event -> {
            controller.stop();

            VlcPlayerFactory.finish();

            try {
                Unirest.shutdown();
            } catch (IOException ex) {
                Logger.getLogger(Projector.class.getName()).log(Level.SEVERE, null, ex);
            }

            SwingUtilities.invokeLater(() -> Platform.runLater(() -> System.exit(0)));
        });

        new Thread(this).start();
    }
}
