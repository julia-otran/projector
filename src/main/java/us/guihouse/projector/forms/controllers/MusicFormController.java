package us.guihouse.projector.forms.controllers;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class MusicFormController implements Initializable {

    @FXML
    private TextField titleTextField;
    
    @FXML
    private ComboBox artistCombo;
    
    @FXML
    private TextArea musicTextArea;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
    @FXML
    public void onCreateArtist() {
        
    }
    
    @FXML
    public void onCancel() {
        
    }
    
    @FXML
    public void onSave() {
        
    }
}
