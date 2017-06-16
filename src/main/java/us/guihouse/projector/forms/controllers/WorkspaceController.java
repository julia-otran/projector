/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.AnchorPane;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class WorkspaceController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        new GraphicsDeviceMenuHelper(projectionScreenMenu);
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
    public void onRegisterManualMusic() {
        
    }
    
    @FXML
    public void onImportMusicFromWeb() {
        
    }
    
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
    private AnchorPane targetAnchorPane;
}
