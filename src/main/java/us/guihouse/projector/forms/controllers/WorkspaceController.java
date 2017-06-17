/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import us.guihouse.projector.projection.ProjectionWebView;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class WorkspaceController implements Initializable {
    private GraphicsDeviceHelper graphicsHelper;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graphicsHelper = new GraphicsDeviceHelper(projectionScreenMenu);
        ProjectionWebView pwv = graphicsHelper.getProjectionManager().createWebView();
        
        targetPane.getChildren().add(pwv.getNode());
        pwv.getNode().setScaleX(0.8);
        pwv.getNode().setScaleY(0.8);
        pwv.getNode().setLayoutX(0);
        pwv.getNode().setLayoutY(0);
        pwv.getWebView().getEngine().load("https://www.google.com");
        
        graphicsHelper.getProjectionManager().setWebView(pwv);
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
    
    @FXML
    public void onAddMusic() {}
    
    @FXML
    public void onAddBrowser() {}
    
    @FXML
    public void onAddPicture() {}
    
    @FXML
    public void onAddText() {}
    
    // ------------------------------
    // Preview
    // ------------------------------
    
    @FXML
    private Canvas previewCanvas;
    
    @FXML
    private Pane targetPane;

}
