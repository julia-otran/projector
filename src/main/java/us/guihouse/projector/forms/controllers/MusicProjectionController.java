/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Callback;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrappedText;
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
    private TableView<WrappedText> phrasesTable;

    @FXML
    private TextField searchTextField;

    private ManageMusicService manageMusicService;
    private Music music;
    private Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();

    private final ObservableList<WrappedText> data = FXCollections.observableArrayList();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TableColumn<WrappedText, List<String>> col = (TableColumn<WrappedText, List<String>>) phrasesTable.getColumns().get(0);
        col.setCellValueFactory(new PropertyValueFactory<>("lines"));
        col.setCellFactory(new Callback<TableColumn<WrappedText, List<String>>, TableCell<WrappedText, List<String>>>() {
            @Override
            public TableCell<WrappedText, List<String>> call(TableColumn<WrappedText, List<String>> param) {
                return new MusicPhraseCell();
            }
        });

        phrasesTable.setItems(data);
    }

    @Override
    public void initWithProjectionManager(final ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);

        notifyTitleChange(music.getNameWithArtistProperty().getValue());

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

        phrasesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        phrasesTable.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (newValue == null) {
                    projectionManager.setText(WrappedText.blankText());
                    return;
                }

                int pos = newValue.intValue();
                if (pos < 0 || pos >= data.size()) {
                    projectionManager.setText(WrappedText.blankText());
                    return;
                }

                projectionManager.setText(data.get(pos));
            }
        });

        reprocessPhrases();
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

    public int getMusicId() {
        return this.music.getId();
    }

    public Property<TextWrapper> getTextWrapperProperty() {
        return textWrapperProperty;
    }

    private void reprocessPhrases() {
        data.clear();
        data.addAll(textWrapperProperty.getValue().fitGroups(music.getPhrasesList()));
    }

    class MusicPhraseCell extends TableCell<WrappedText, List<String>> {

        private VBox box;

        public MusicPhraseCell() {
            super();
            box = new VBox();
            box.setPadding(new Insets(5, 0, 5, 0));
        }

        @Override
        protected void updateItem(List<String> item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setGraphic(null);
            } else {
                ensureLabels(item.size());

                int i = 0;
                for (String s : item) {
                    ((Label) box.getChildren().get(i)).setText(s);
                    i++;
                }

                setGraphic(box);
            }

            setText(null);
        }

        private final void ensureLabels(int count) {
            while (box.getChildren().size() < count) {
                box.getChildren().add(createLabel());
            }
        }

        private final Label createLabel() {
            Label label = new Label();
            VBox.setVgrow(label, Priority.ALWAYS);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            Font original = label.getFont();
            label.setFont(Font.font(original.getFamily(), original.getSize() + 4));
            return label;
        }
    }
}
