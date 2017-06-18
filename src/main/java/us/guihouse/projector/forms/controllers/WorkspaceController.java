/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import us.guihouse.projector.scenes.SceneObserver;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Pane;
import us.guihouse.projector.scenes.BrowserSubScene;
import us.guihouse.projector.scenes.ImageSubScene;
import us.guihouse.projector.scenes.ProjectionItemSubScene;
import us.guihouse.projector.scenes.TextSubScene;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class WorkspaceController implements Initializable, SceneObserver {
    private GraphicsDeviceHelper graphicsHelper;
    private final List<ProjectionItemSubScene> items = new ArrayList<>(); 
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graphicsHelper = new GraphicsDeviceHelper(projectionScreenMenu);
        initializeProjectionList();
    }    
    
    public void stop() {
        graphicsHelper.stop();
    }
    
    public void dispose() {
        graphicsHelper.dispose();
    }
    
    // ------------------------------
    // Menu
    // ------------------------------
    @FXML
    private CheckMenuItem cropBackgroundMenuItem;
    
    @FXML
    private RadioMenuItem singleLineProjectionMenuItem;
    
    @FXML
    private RadioMenuItem multilineProjectionMenuItem;
    
    @FXML
    private Menu projectionScreenMenu;
    
    @FXML
    public void onRegisterManualMusic() { }
    
    @FXML
    public void onImportMusicFromWeb() { }
    
    @FXML
    public void onImportMusicFromTextFile() {}
    
    @FXML
    public void onOpenMusicList() {}
    
    @FXML
    public void onSelectBackgroundImageFile() {}
    
    @FXML
    public void onCropBackgroundChanged() {}
    
    @FXML
    public void onChangeFont() {}
    
    @FXML
    public void onChangeFullScreen() {}
    
    @FXML
    public void onSingleLineProjection() {}
    
    @FXML
    public void onMultilineProjection() {}
    
    @FXML
    public void onHelpAbout() {}
    
    @FXML
    public void onHelpManual() {}
    
    // ------------------------------
    // Projection List
    // ------------------------------
    
    @FXML
    private ListView projectionListView;
    
    private boolean changingTitle = false;
    
    private void initializeProjectionList() {
        projectionListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        projectionListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (changingTitle) { return; }
                
                if (newValue != null) {
                    int val = newValue.intValue();
                    if (val >= 0 && val < items.size()) {
                        setProjectionView(items.get(val));
                        return;
                    }
                }
                
                setProjectionView(null);
            }
        });
    }
    
    private void setProjectionView(ProjectionItemSubScene scene) {
        if (scene == null) {
            targetPane.getChildren().clear();
            return;
        }
        
        if (targetPane.getChildren().size() <= 0) {
            targetPane.getChildren().add(scene);
            return;
        }
        
        if (Objects.equals(targetPane.getChildren().get(0), scene)) {
            return;
        }
        
        targetPane.getChildren().clear();
        targetPane.getChildren().add(scene);
    }
    
    @FXML
    public void onAddMusic() {}
    
    @FXML
    public void onAddBrowser() {
        try {
            ProjectionItemSubScene created = BrowserSubScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);
            
            items.add(created);
            projectionListView.getItems().add("Página Web");
            projectionListView.getSelectionModel().select(projectionListView.getItems().size() - 1);
            
            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    public void onAddPicture() {
        try {
            ProjectionItemSubScene created = ImageSubScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);
            
            items.add(created);
            projectionListView.getItems().add("Nova imagem");
            projectionListView.getSelectionModel().select(projectionListView.getItems().size() - 1);
            
            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    public void onAddText() {
        try {
            ProjectionItemSubScene created = TextSubScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);
            
            items.add(created);
            projectionListView.getItems().add("Novo texto");
            projectionListView.getSelectionModel().select(projectionListView.getItems().size() - 1);
            
            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // ------------------------------
    // Preview
    // ------------------------------
    
    @FXML
    private Canvas previewCanvas;
    
    @FXML
    private Pane targetPane;

    @Override
    public void titleChanged(ProjectionItemSubScene scene, String newTitle) {
        int index = items.indexOf(scene);
        if (index >= 0 && index < items.size()) {
            changingTitle = true;
            int selected = projectionListView.getSelectionModel().getSelectedIndex();
            projectionListView.getItems().set(index, newTitle);
            projectionListView.getSelectionModel().select(selected);
            changingTitle = false;
        }
    }

}
