/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import com.sun.javafx.binding.ObjectConstant;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.repositories.MusicRepository;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class MusicListController implements Initializable {

    private MusicRepository musicRepository = new MusicRepository();
    
    @FXML
    private TextField searchText;
    
    @FXML
    private TableView resultsTable;
   
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        
        searchText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                fillMusicsProtected(newValue);
            }
        });
        
        fillMusicsProtected(null);
    }    
    
    private void initTable() {
        resultsTable.getColumns().clear();
        
        TableColumn nameCol = new TableColumn("Título");
        nameCol.setCellValueFactory(new PropertyValueFactory<Music,String>("name"));
        resultsTable.getColumns().add(nameCol);
        
        TableColumn artistCol = new TableColumn("Artista");
        artistCol.setCellValueFactory(new Callback<CellDataFeatures<Music, String>, ObservableValue<String>>() {  
            @Override  
            public ObservableValue<String> call(CellDataFeatures<Music, String> data){  
                if (data.getValue().getArtist() != null) {
                    return new SimpleStringProperty(data.getValue().getArtist().getName());
                }
                return new SimpleStringProperty("");
            }  
        });
        resultsTable.getColumns().add(artistCol);
        
        TableColumn phrasesCol = new TableColumn("Letra");
        phrasesCol.setCellValueFactory(new Callback<CellDataFeatures<Music, String>, ObservableValue<String>>() {  
            @Override  
            public ObservableValue<String> call(CellDataFeatures<Music, String> data){  
                if (!data.getValue().getPhrases().isEmpty()) {
                    return new SimpleStringProperty(data.getValue().getPhrases().get(0));
                }
                return new SimpleStringProperty("");
            }  
        });
        
        resultsTable.getColumns().add(phrasesCol);
        resultsTable.requestFocus();
    }
    
    @FXML
    public void onManualType() {
        
    }
    
    @FXML
    public void onWebImport() {
        
    }
    
    private void fillMusicsProtected(String term) {
        try {
            fillMusics(term);
        } catch (SQLException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void fillMusics(String term) throws SQLException {
        List<Music> musics;
        
        if (term == null || term.isEmpty()) {
            musics = musicRepository.listAll();
        } else {
            musics = musicRepository.listByTerm(term);
        }
        
        resultsTable.getItems().clear();
        resultsTable.getItems().addAll(musics);
    }
}
