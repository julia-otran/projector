package us.guihouse.projector.forms.controllers;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import us.guihouse.projector.services.ManageMusicService;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class MusicFormController implements Initializable {
    private ManageMusicService musicService;
    private BackCallback callback;
    private String creatingArtist;
    
    @FXML
    private TextField titleTextField;
    
    @FXML
    private ComboBox<String> artistCombo;
    
    @FXML
    private TextArea musicTextArea;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }    
    
    @FXML
    public void onCreateArtist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Criar artista");
        dialog.setHeaderText("Incluir um novo artista");
        dialog.setContentText("Digite o nome do artista:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmed = name.trim();
            
            if (trimmed.isEmpty()) {
                creatingArtist = null;
            } else {
                creatingArtist = trimmed;
            }
            
            populateArtists();
        });
    }
    
    @FXML
    public void onCancel() {
        getCallback().goBack();
    }
    
    @FXML
    public void onSave() {
        String name = titleTextField.getText();
        String artist = artistCombo.getSelectionModel().getSelectedItem();
        String music = musicTextArea.getText();
        
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText("Falha ao salvar música.");
        
        try {
            musicService.createMusic(name, artist, music);
            getCallback().goBackAndRefresh();
        } catch (ManageMusicService.MusicAlreadyPresentException ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Já existe uma música com mesmo nome e artista.");
            alert.showAndWait();
        } catch (ManageMusicService.PersistenceException ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Erro desconhecido de persistência. Verifique a base de dados.");
            alert.showAndWait();
        } catch (ManageMusicService.InavlidArtist ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Verifique o nome do artista.");
            alert.showAndWait();
        } catch (ManageMusicService.InvalidName ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Verifique o título da música.");
            alert.showAndWait();
        } catch (ManageMusicService.InvalidPhrases ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Verifique a letra.");
            alert.showAndWait();
        }
    }

    public void setManageMusicService(ManageMusicService manageMusicService) {
        this.musicService = manageMusicService;
    }

    public void setBackCallback(BackCallback callback) {
        this.callback = callback;
    }

    public BackCallback getCallback() {
        return callback;
    }

    public void init() {
        populateArtists();
    }

    private void populateArtists() {
        ObservableList<String> list = FXCollections.observableArrayList(musicService.listArtists());
        
        if (creatingArtist != null) {
            list.add(0, creatingArtist);
            artistCombo.setItems(list);
            artistCombo.getSelectionModel().select(creatingArtist);
        } else {
            artistCombo.setItems(list);
        }
    }
}
