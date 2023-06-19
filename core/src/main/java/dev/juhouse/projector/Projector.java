/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector;

import com.mashape.unirest.http.Unirest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.juhouse.projector.forms.controllers.GraphicsDeviceHelper;
import dev.juhouse.projector.forms.controllers.SceneManager;
import dev.juhouse.projector.forms.controllers.WorkspaceController;
import dev.juhouse.projector.other.SQLiteJDBCDriverConnection;
import dev.juhouse.projector.utils.ThemeFinder;
import dev.juhouse.projector.utils.VlcPlayerFactory;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.log.LogLevel;

import static dev.juhouse.projector.utils.FilePaths.PROJECTOR_LOCK_FILE_PATH;

/**
 *
 * @author 15096134
 */
public class Projector extends Application implements Runnable {
    private GraphicsDeviceHelper graphicsHelper;

    private WorkspaceController controller;

    private FileChannel lockFileChannel;

    public boolean checkIfIsRunning() {
        try {
            // Must not close this
            lockFileChannel = FileChannel.open(PROJECTOR_LOCK_FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = lockFileChannel.tryLock();

            if (lock == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro");
                alert.setHeaderText("O programa já está aberto.");
                alert.setContentText("Clique no programa que está na barra de tarefas, ou reinicie o computador.");
                alert.showAndWait();
                System.exit(0);
                return true;
            }
        } catch (IOException ex) {
            Logger.getLogger(Projector.class.getName()).log(Level.WARNING, "Lock for prevent multiple instances failed.", ex);
        }

        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void run() {
        ThemeFinder.loadThemes();

        Platform.runLater(() -> {
            graphicsHelper = new GraphicsDeviceHelper();
            graphicsHelper.start();

            controller.init(graphicsHelper);
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (checkIfIsRunning()) {
            return;
        }

        SQLiteJDBCDriverConnection.connect();
        SQLiteJDBCDriverConnection.migrate();

        primaryStage.setTitle("Projector");
        primaryStage.setMaxWidth(Double.MAX_VALUE);
        primaryStage.setMaxHeight(Double.MAX_VALUE);
        
        URL url = getClass().getClassLoader().getResource("fxml/workspace.fxml");
        
        FXMLLoader loader = new FXMLLoader(url);
        
        Parent workspaceRoot = loader.load();
        controller = loader.getController();
        
        Scene workspaceScene = new Scene(workspaceRoot, 1000, 600);
        
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

            @Override
            public HostServices getHostServices() {
                return Projector.this.getHostServices();
            }
        });
        
        workspaceScene.addEventFilter(KeyEvent.KEY_PRESSED, t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                controller.onEscapeKeyPressed();
            }
        });
        
        primaryStage.setScene(workspaceScene);

        primaryStage.setMaximized(true);

        primaryStage.show();
        
        primaryStage.setOnCloseRequest(event -> {
            controller.stop();
            VlcPlayerFactory.finish();

            try {
                Unirest.shutdown();
            } catch (IOException ex) {
                Logger.getLogger(Projector.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                lockFileChannel.close();
            } catch (IOException e) {
                Logger.getLogger(Projector.class.getName()).log(Level.SEVERE, null, e);
            }

            Platform.runLater(() -> System.exit(0));
        });

        new Thread(this).start();
    }
}
