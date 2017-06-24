/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.services.ManageMusicService;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class MusicProjectionController extends ProjectionController {

    @FXML
    private Button clearScreenButton;
    
    @FXML
    private TableView phrasesTable;
    
    @FXML
    private TextField searchTextField;
    
    private ManageMusicService manageMusicService;
    private Music music;
    private Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }    

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager); 
        
        music.getNameWithArtistProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                notifyTitleChange(newValue);
            }
        });

        music.getPhrasesList().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends String> c) {
                reprocessPhrases();
            }
        });
        
        textWrapperProperty.addListener(new ChangeListener<TextWrapper>() {
            @Override
            public void changed(ObservableValue<? extends TextWrapper> observable, TextWrapper oldValue, TextWrapper newValue) {
                reprocessPhrases();
            }
        });
    }
    
    @FXML
    private void onSearchFieldKeyPress() {
        
    }

    public void setManageMusicService(ManageMusicService svc) {
        this.manageMusicService = svc;
    }
    
    public void loadMusicWithId(Integer musicId) {
        try {
            this.music = manageMusicService.openMusicForPlay(musicId);
        } catch (ManageMusicService.PersistenceException ex) {
            Logger.getLogger(MusicProjectionController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Property<TextWrapper> getTextWrapperProperty() {
        return textWrapperProperty;
    }
    
    private void reprocessPhrases() {
        System.out.println("reprocessPhrases");
    }
}
