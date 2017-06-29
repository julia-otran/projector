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
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
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
    private TableView<SelectionText> phrasesTable;

    @FXML
    private TextField searchTextField;

    private ManageMusicService manageMusicService;
    private Music music;
    private Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();

    private final ObservableList<SelectionText> data = FXCollections.observableArrayList();

    private String clearText;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TableColumn<SelectionText, List<String>> col = (TableColumn<SelectionText, List<String>>) phrasesTable.getColumns().get(0);
        col.setCellValueFactory(new PropertyValueFactory<>("lines"));
        col.setCellFactory(new Callback<TableColumn<SelectionText, List<String>>, TableCell<SelectionText, List<String>>>() {
            @Override
            public TableCell<SelectionText, List<String>> call(TableColumn<SelectionText, List<String>> param) {
                return new MusicPhraseCell();
            }
        });

        TableColumn<SelectionText, BooleanProperty> sel = new TableColumn<>("");
        sel.setCellValueFactory(new PropertyValueFactory<>("selected"));
        sel.setCellFactory(column -> {
            return new TableCell<SelectionText, BooleanProperty>() {
                @Override
                protected void updateItem(BooleanProperty item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(null);
                    setText(null);

                    TableRow<SelectionText> row = getTableRow();

                    if (row != null && !empty) {
                        if (item.get()) {
                            row.setStyle("-fx-background-color:lightcoral");
                        } else {
                            row.setStyle("");
                        }
                    }
                }
            };
        });

        phrasesTable.getColumns().add(sel);
        phrasesTable.setItems(data);
        clearText = clearScreenButton.getText();
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
                clearMarker();
                
                if (newValue == null) {
                    disableClear();
                    projectionManager.setText(WrappedText.blankText());
                    return;
                }

                enableClear();

                int pos = newValue.intValue();
                if (pos < 0 || pos >= data.size()) {
                    projectionManager.setText(WrappedText.blankText());
                    return;
                }

                WrappedText text = data.get(pos).text;

                if (text.isEmpty()) {
                    if (oldValue != null) {
                        if (oldValue.intValue() < pos) {
                            pos++;
                        } else {
                            pos--;
                        }

                        if (pos >= 0 && pos < data.size()) {
                            final int pos2 = pos;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    phrasesTable.getSelectionModel().select(pos2);
                                }
                            });
                        }
                    }
                } else {
                    projectionManager.setText(text);
                }
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
        List<WrappedText> lst = textWrapperProperty.getValue().fitGroups(music.getPhrasesList());
        data.addAll(lst.stream().map(wt -> new SelectionText(wt)).collect(Collectors.toList()));
    }

    private void disableClear() {
        clearScreenButton.setText(clearText);
        clearScreenButton.disableProperty().set(true);
    }

    private void enableClear() {
        clearScreenButton.setText("Limpar Tela (ESC)");
        clearScreenButton.disableProperty().set(false);
    }

    @FXML
    private void onClearScreen() {
        Integer current = phrasesTable.getSelectionModel().getSelectedIndex();
        
        phrasesTable.getSelectionModel().clearSelection();
        
        if (current >= 0 && current < data.size()) {
            SelectionText st = data.get(current);
            st.selected.setValue(Boolean.TRUE);
            data.set(current, st);
        }
    }
    
    private void clearMarker() {
        for (int i=0; i<data.size(); i++) {
            SelectionText st = data.get(i);
            if (st.selected.get()) {
                st.selected.set(false);
                data.set(i, st);
            }
        }
    }

    public static class SelectionText {

        public SelectionText(WrappedText text) {
            this.text = text;
            this.lines = FXCollections.observableArrayList(text.getLines());
            this.selected = new SimpleBooleanProperty(false);
        }

        WrappedText text;
        BooleanProperty selected;
        ObservableList<String> lines;

        public List<String> getLines() {
            return lines;
        }

        public BooleanProperty getSelected() {
            return selected;
        }
    }

    class MusicPhraseCell extends TableCell<SelectionText, List<String>> {

        private final VBox box;

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

                int p = 0;
                for (Node n : box.getChildren()) {
                    if (p < item.size()) {
                        ((Label) n).setText(item.get(p));
                    } else {
                        ((Label) n).setText("");
                    }

                    p++;
                }

                setGraphic(box);
            }

            setText(null);
        }

        private void ensureLabels(int count) {
            while (box.getChildren().size() < count) {
                box.getChildren().add(createLabel());
            }
            while (box.getChildren().size() > count) {
                box.getChildren().remove(0);
            }
        }

        private final Label createLabel() {
            Label label = new Label();
            VBox.setVgrow(label, Priority.ALWAYS);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            label.setBackground(Background.EMPTY);
            Font original = label.getFont();
            label.setFont(Font.font(original.getFamily(), original.getSize() + 4));
            return label;
        }
    }
}
