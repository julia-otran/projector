/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.text.Collator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
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
import us.guihouse.projector.other.ProjectorStringUtils;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.services.ManageMusicService;
import us.guihouse.projector.utils.ThemeFinder;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class MusicProjectionController extends ProjectionController {
    public static final String MUSIC_ID_PROPERTY = "MUSIC_ID";

    @FXML
    private Button clearScreenButton;

    @FXML
    private Button removeBackgroundButton;

    @FXML
    private TableView<SelectionText> phrasesTable;

    @FXML
    private TextField searchTextField;

    private ManageMusicService manageMusicService;
    private Music music;
    private Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();

    private ObservableList<SelectionText> data;

    private String clearText;
    final Collator collator = Collator.getInstance();
    private File theme;
    private boolean projecting = false;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        data = phrasesTable.getItems();

        TableColumn<SelectionText, List<String>> col = (TableColumn<SelectionText, List<String>>) phrasesTable.getColumns().get(0);
        col.setCellValueFactory(new PropertyValueFactory<>("lines"));
        col.setCellFactory(new Callback<TableColumn<SelectionText, List<String>>, TableCell<SelectionText, List<String>>>() {
            @Override
            public TableCell<SelectionText, List<String>> call(TableColumn<SelectionText, List<String>> param) {
                return new MusicPhraseCell();
            }
        });

        phrasesTable.setRowFactory(row -> new TableRow<SelectionText>() {
            @Override
            protected void updateItem(SelectionText item, boolean empty) {
                super.updateItem(item, empty);

                if (row == null || empty) {
                    setStyle("");
                    return;
                }

                if (item.selected.get()) {
                    setStyle("-fx-control-inner-background:#ffff00; -fx-control-inner-background-alt:ffff00;");
                } else if (item.firstLine.get()) {
                    setStyle("-fx-control-inner-background:#e6f3ff; -fx-control-inner-background-alt:#e6f3ff;");
                } else {
                    setStyle("");
                }
            }
        });

        clearText = clearScreenButton.getText();
    }

    @Override
    public void initWithProjectionManager(final ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        loadMusicWithId(Integer.parseInt(getObserver().getProperty(MUSIC_ID_PROPERTY)));

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

        music.getThemeProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                setTheme(ThemeFinder.getThemeByVideoName(newValue).getVideoFile());
            }
        });

        textWrapperProperty.addListener(new ChangeListener<TextWrapper>() {
            @Override
            public void changed(ObservableValue<? extends TextWrapper> observable, TextWrapper oldValue, TextWrapper newValue) {
                reprocessPhrases();
            }
        });

        searchTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                focusToTerm(newValue);
            }
        });

        phrasesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        phrasesTable.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (newValue == null) {
                    performClear();
                    return;
                }

                int pos = newValue.intValue();

                if (pos < 0 || pos >= data.size()) {
                    performClear();
                    return;
                }

                enableClear();
                clearMarker();

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
                    projecting = true;
                    projectionManager.setText(text);
                    playTheme();
                }
            }
        });

        reprocessPhrases();
        setTheme(ThemeFinder.getThemeByVideoName(music.getTheme()).getVideoFile());
    }

    private void setTheme(File theme) {
        this.theme = theme;

        if (projecting) {
            playTheme();
        }
    }

    private void playTheme() {
        projectionManager.setMusicForBackground(music.getId(), theme);
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

        SelectionText st;
        WrappedText wt;

        for (int i = 0; i < lst.size(); i++) {
            wt = lst.get(i);
            st = new SelectionText(wt);
            int j = i - 1;

            if (j == -1) {
                st.firstLine.set(true);
            } else if (lst.get(j).isEmpty()) {
                st.firstLine.set(true);
            }

            data.add(st);
        }
    }

    private void markIfPosible(final Number position) {
        if (position == null) {
            return;
        }

        int n = position.intValue();
        if (n < 0 || n >= data.size()) {
            return;
        }

        SelectionText st = data.get(n);
        st.selected.set(true);
        phrasesTable.refresh();
    }

    private void performClear() {
        projecting = false;
        clearScreenButton.setText(clearText);
        clearScreenButton.disableProperty().set(true);
        projectionManager.setText(WrappedText.blankText());
    }

    private void enableClear() {
        clearScreenButton.setText("Limpar Tela (ESC)");
        clearScreenButton.disableProperty().set(false);
        removeBackgroundButton.disableProperty().set(false);
    }

    @FXML
    public void onClearScreen() {
        markIfPosible(phrasesTable.getSelectionModel().getSelectedIndex());
        phrasesTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void removeBackground() {
        projectionManager.setMusicForBackground(null, null);
        removeBackgroundButton.disableProperty().set(true);
    }

    private void clearMarker() {
        data.stream().filter((st) -> (st.selected.get())).forEach((st) -> {
            st.selected.set(false);
        });
        phrasesTable.refresh();
    }

    private void focusToTerm(String term) {
        if (term == null) {
            return;
        }

        term = ProjectorStringUtils.noAccents(term.trim());

        if (term.isEmpty()) {
            return;
        }
        
        int closestPhrase = getSelectedOrMarkedPhraseIndex();
        if (closestPhrase < 0 || closestPhrase >= data.size()) {
            closestPhrase = 0;
        } 
        
        int positions[] = new int[data.size()];
        boolean neg = false;
        
        for (int i=0; i<data.size(); i++) {
            if (i % 2 == 0) {
                positions[i] = closestPhrase - (i / 2);
            } else {
                positions[i] = closestPhrase + ((i+1) / 2);
            }
            
            neg |= positions[i] < 0;
            
            if (neg) {
                positions[i] = i;
            }
        }

        for (int i = 0; i < data.size(); i++) {
            SelectionText st = data.get(positions[i]);
            if (st.hasTerm(term)) {
                phrasesTable.scrollTo(st);
                markIfPosible(positions[i]);
                return;
            }
        }
    }
    
    private int getSelectedOrMarkedPhraseIndex() {
        int selected = phrasesTable.getSelectionModel().getSelectedIndex();
        
        if (selected < 0 || selected >= data.size()) {
            selected = phrasesTable.getSelectionModel().getFocusedIndex();
        }
        
        if (selected < 0 || selected >= data.size()) {
            for (int i=0; i<data.size(); i++) {
                if (data.get(i).selectedProperty().get()) {
                    return i;
                }
            }
        }
        
        return selected;
    }

    public static class SelectionText {

        public SelectionText(WrappedText text) {
            this.text = text;
            this.lines = FXCollections.observableArrayList(text.getLines());
            this.selected = new SimpleBooleanProperty(false);
            this.firstLine = new SimpleBooleanProperty(false);
        }

        private final WrappedText text;
        private final SimpleBooleanProperty selected;
        private final SimpleBooleanProperty firstLine;
        private final ObservableList<String> lines;

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public SimpleBooleanProperty firstLineProperty() {
            return firstLine;
        }

        public List<String> getLines() {
            return lines;
        }

        private boolean hasTerm(String term) {
            return ProjectorStringUtils.noAccents(text.getJoinedLines()).contains(term);
        }
    }

    public static class MusicPhraseCell extends TableCell<SelectionText, List<String>> {

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

        private Label createLabel() {
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
    
    
    @Override
    public void onEscapeKeyPressed() {
        if (!clearScreenButton.isDisabled()) {
            clearScreenButton.fire();
        }
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        if (!removeBackgroundButton.isDisabled()) {
            removeBackgroundButton.fire();
        }
    }
}
