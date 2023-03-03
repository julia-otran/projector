/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.juhouse.projector.dtos.ImportingMusicDTO;
import dev.juhouse.projector.dtos.ListMusicDTO;
import dev.juhouse.projector.music_importing.ImportCallback;
import dev.juhouse.projector.music_importing.ImportMusicContext;
import dev.juhouse.projector.music_importing.MusicUrlImporter;
import dev.juhouse.projector.scenes.MusicFormScene;
import dev.juhouse.projector.services.ManageMusicService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class MusicListController implements Initializable, BackCallback, ImportCallback {

    @FXML
    private TextField searchText;

    @FXML
    private TableView<ListMusicDTO> resultsTable;

    private AddMusicCallback addCallback;
    private ManageMusicService manageMusicService;

    private Stage currentStage;
    private Parent oldRoot;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();

        searchText.textProperty().addListener((observable, oldValue, newValue) -> fillMusicsProtected(newValue));

        resultsTable.setRowFactory(tv -> {
            TableRow<ListMusicDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    ListMusicDTO rowData = row.getItem();
                    addToList(rowData.getId());
                }
            });
            return row;
        });
    }

    private void initTable() {
        resultsTable.getColumns().clear();

        resultsTable.getColumns().add(getActionsColumn());

        TableColumn<ListMusicDTO, String> nameCol = new TableColumn<>("Título");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultsTable.getColumns().add(nameCol);

        TableColumn<ListMusicDTO, String> artistCol = new TableColumn<>("Artista");
        artistCol.setCellValueFactory(new PropertyValueFactory<>("artistName"));
        resultsTable.getColumns().add(artistCol);

        TableColumn<ListMusicDTO, String> phrasesCol = new TableColumn<>("Letra");
        phrasesCol.setCellValueFactory(new PropertyValueFactory<>("phrases"));
        resultsTable.getColumns().add(phrasesCol);

        resultsTable.requestFocus();
    }

    @FXML
    public void onManualType() {
        try {
            Parent root = MusicFormScene.createMusicFormScene(manageMusicService, this);
            oldRoot = currentStage.getScene().getRoot();
            currentStage.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void onWebImport() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Importar música");
        dialog.setHeaderText("Copie e cole a URL (endereço) da música no campo abaixo:");
        dialog.setContentText("URL:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            MusicUrlImporter importer = ImportMusicContext.getContext().getImporter(url);
            if (importer == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro");
                alert.setHeaderText("Falha ao importar música.");
                alert.setContentText("URL (endereço) não suportada. Usar letras.mus.br ou vagalume.com.br");
                alert.showAndWait();
            } else {
                importer.execute(this);
            }
        });
    }

    private void fillMusicsProtected(String term) {
        try {
            fillMusics(term);
        } catch (ManageMusicService.PersistenceException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void fillMusics(String term) throws ManageMusicService.PersistenceException {
        List<ListMusicDTO> musics = manageMusicService.listByTermIfPresentWithLimit(term);

        resultsTable.getItems().clear();
        resultsTable.getItems().addAll(musics);
    }

    private TableColumn<ListMusicDTO, Integer> getActionsColumn() {
        TableColumn<ListMusicDTO, Integer> col = new TableColumn<>("Açoes");

        col.setCellValueFactory(new PropertyValueFactory<>("id"));
        col.setCellFactory(param -> new ActionsTableCell());

        col.prefWidthProperty().setValue(300);

        return col;
    }

    public void setAddMusicCallback(AddMusicCallback callback) {
        this.addCallback = callback;
    }

    public void setManageMusicService(ManageMusicService manageMusicService) {
        this.manageMusicService = manageMusicService;

        try {
            fillMusics(null);
        } catch (ManageMusicService.PersistenceException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void goBack() {
        currentStage.getScene().setRoot(oldRoot);
    }

    @Override
    public void goBackWithId(Integer id) {
        fillMusicsProtected(searchText.getText());
        addToList(id);
        currentStage.getScene().setRoot(oldRoot);
    }

    @Override
    public void goBackAndReload() {
        fillMusicsProtected(searchText.getText());
        currentStage.getScene().setRoot(oldRoot);
    }

    @Override
    public void onImportSuccess(ImportingMusicDTO music) {
        try {
            if (manageMusicService.alreadyExists(music)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro");
                alert.setHeaderText("Falha ao importar música.");
                alert.setContentText("A música já existe, pesquise-a e inclua-a na lista.");
                alert.showAndWait();
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Parent root = MusicFormScene.createMusicFormScene(manageMusicService, this, music);
            oldRoot = currentStage.getScene().getRoot();
            currentStage.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onImportError(boolean errored) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText("Falha ao importar música.");
        alert.setContentText("Falha ao obter dados.");
        alert.showAndWait();
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
            actionsBox.spacingProperty().set(5);
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
        if (this.addCallback != null) {
            if (!this.addCallback.addMusic(id)) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erro");
                a.setHeaderText("Falha incluir item");
                a.setContentText("Esta música já esta na lista.");
                a.show();
            }
        }
    }

    private void seeDetails(Integer id) {
        System.out.println("seeDetails " + id);
    }

    private void edit(Integer id) {
        try {
            Parent root = MusicFormScene.editMusicFormScene(manageMusicService, this, id);
            oldRoot = currentStage.getScene().getRoot();
            currentStage.getScene().setRoot(root);
        } catch (IOException | ManageMusicService.PersistenceException ex) {
            Logger.getLogger(MusicListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

}
