/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import us.guihouse.projector.dtos.ListMusicDTO;
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
    private TableView<ListMusicDTO> resultsTable;
   
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
        
        resultsTable.getColumns().add(getActionsColumn());
        
        TableColumn<ListMusicDTO,String> nameCol = new TableColumn<>("Título");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultsTable.getColumns().add(nameCol);
        
        TableColumn<ListMusicDTO,String> artistCol = new TableColumn<>("Artista");
        artistCol.setCellValueFactory(new PropertyValueFactory<>("artistName"));
        resultsTable.getColumns().add(artistCol);
        
        TableColumn<ListMusicDTO,String> phrasesCol = new TableColumn<>("Letra");
        phrasesCol.setCellValueFactory(new PropertyValueFactory<>("phrases"));
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
        List<ListMusicDTO> musics;
        
        if (term == null || term.isEmpty()) {
            musics = musicRepository.listWithLimit();
        } else {
            musics = musicRepository.listByTermWithLimit(term);
        }
        
        resultsTable.getItems().clear();
        resultsTable.getItems().addAll(musics);
    }
    
    private TableColumn<ListMusicDTO, Integer> getActionsColumn() {
        TableColumn<ListMusicDTO, Integer> col = new TableColumn<>("Açoes");
        
        col.setCellValueFactory(new PropertyValueFactory<>("id"));
        col.setCellFactory(new Callback<TableColumn<ListMusicDTO, Integer>, TableCell<ListMusicDTO, Integer>>() {
            @Override
            public TableCell<ListMusicDTO, Integer> call(TableColumn<ListMusicDTO, Integer> param) {
                return new ActionsTableCell();
            }
        });

        return col;
    }
    
    class ActionsTableCell extends TableCell<ListMusicDTO, Integer> implements EventHandler<ActionEvent> {
        private final Button addToList = new Button("Incluir a lista");
        private final Button seeDetails = new Button("Ver Detalhes");
        private final Button edit = new Button("Alterar");
        private final HBox actionsBox = new HBox();
        private Integer id;

        private ActionsTableCell() {
            super();
            actionsBox.getChildren().addAll(addToList, seeDetails, edit);
            addToList.setOnAction(this);
            seeDetails.setOnAction(this);
            edit.setOnAction(this);
        }
        
        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(actionsBox);
            }
            
            setText(null);
            this.id = item;
        }

        @Override
        public void handle(ActionEvent event) {
            if (addToList.equals(event.getSource())) {
                addToList(id);
            }
            
            if (seeDetails.equals(event.getSource())) {
                seeDetails(id);
            }
            
            if (edit.equals(event.getSource())) {
                edit(id);
            }
        }
    }
    
    private void addToList(Integer id) {
        System.out.println("AddToList " + id);
    }
    
    private void seeDetails(Integer id) {
        System.out.println("seeDetails " + id);
    }
    
    private void edit(Integer id) {
        System.out.println("edit " + id);
    }
}
