/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Callback;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.models.MusicTheme;
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
    private ListView<SelectionText> miniPhrasesListView;

    @FXML
    private TextField searchTextField;

    private ManageMusicService manageMusicService;
    private Music music;
    private final Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();

    private ObservableList<SelectionText> data;

    private String clearText;
    private File theme;
    private boolean projecting = false;

    private Integer markedPosition = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        data = phrasesTable.getItems();

        miniPhrasesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        miniPhrasesListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<SelectionText> call(ListView<SelectionText> selectionTextListView) {
                ListCell<SelectionText> cell = new ListCell<>() {
                    {
                        setStyle("-fx-font-size: 10px;");
                    }

                    @Override
                    protected void updateItem(SelectionText selectionText, boolean empty) {
                        if (empty) {
                            setText("");
                            setGraphic(null);
                        } else {
                            setText(selectionText.text.getJoinedLines());
                        }
                    }

                    @Override
                    public void updateSelected(boolean b) {
                        super.updateSelected(b);

                        if (b) {
                            setStyle("-fx-background-color: -fx-focus-color");
                        } else {
                            setStyle("");
                        }
                    }
                };

                cell.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                        phrasesTable.getSelectionModel().select(cell.getIndex());
                        phrasesTable.scrollTo(cell.getIndex());
                        phrasesTable.requestFocus();
                    }
                });

                return cell;
            }
        });

        TableColumn<SelectionText, List<String>> col = (TableColumn<SelectionText, List<String>>) phrasesTable.getColumns().get(0);
        col.setCellValueFactory(new PropertyValueFactory<>("lines"));
        col.setCellFactory(param -> new MusicPhraseCell());
        col.prefWidthProperty().bind(phrasesTable.widthProperty());

        phrasesTable.setRowFactory(row -> new TableRow<>() {
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

        music.getNameWithArtistProperty().addListener((observable, oldValue, newValue) -> notifyTitleChange(newValue));

        music.getPhrasesList().addListener((ListChangeListener<String>) c -> reprocessPhrases());

        music.getThemeProperty().addListener((observable, oldValue, newValue) -> setTheme(ThemeFinder.getThemeByVideoName(newValue).getVideoFile()));

        textWrapperProperty.addListener((observable, oldValue, newValue) -> reprocessPhrases());

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> focusToTerm(newValue));

        phrasesTable.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.DOWN)) {
                if (markedPosition != null) {
                    int scrollTo = markedPosition;
                    phrasesTable.getSelectionModel().select(scrollTo);
                    phrasesTable.scrollTo(scrollTo);
                    markedPosition = null;
                }
            }
        });

        phrasesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        phrasesTable.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                performClear();
                return;
            }

            int pos = newValue.intValue();

            if (pos < 0 || pos >= data.size()) {
                performClear();
                return;
            }

            miniPhrasesListView.getSelectionModel().clearSelection();
            miniPhrasesListView.getSelectionModel().select(newValue.intValue());

            enableClear();
            clearMarker();

            WrappedText text = data.get(pos).text;

            TableViewSkin<?> skin = (TableViewSkin<?>) phrasesTable.getSkin();
            VirtualFlow<?> flow = ((VirtualFlow<?>) skin.getChildren().get(1));

            if (text.isEmpty()) {
                if (oldValue != null) {
                    if (oldValue.intValue() < pos) {
                        pos++;
                    } else {
                        pos--;
                    }

                    if (pos >= 0 && pos < data.size()) {
                        final int pos2 = pos;
                        Platform.runLater(() -> {
                            phrasesTable.getSelectionModel().select(pos2);
                            if (flow != null) {
                                flow.scrollPixels(50);
                            }
                        });
                    }
                }
            } else {
                if (newValue.equals(0)) {
                    Platform.runLater(() -> markedPosition = null);
                } else {
                    markedPosition = null;
                }
                projecting = true;
                projectionManager.setText(text);
                playTheme();

                if (flow != null) {
                    IndexedCell<?> firstVisibleCell = flow.getFirstVisibleCell();
                    IndexedCell<?> lastVisibleCell = flow.getLastVisibleCell();

                    if (firstVisibleCell != null && lastVisibleCell != null) {
                        int firstIndex = firstVisibleCell.getIndex();
                        int center = firstIndex + ((lastVisibleCell.getIndex() - firstIndex) / 2);

                        if (newValue.intValue() > center && newValue.intValue() > oldValue.intValue()) {
                            phrasesTable.scrollTo(firstIndex + 2);
                        }
                    }
                }
            }
        });

        reprocessPhrases();

        MusicTheme theme = ThemeFinder.getThemeByVideoName(music.getTheme());

        if (theme == null) {
            setTheme(null);
        } else {
            setTheme(theme.getVideoFile());
        }
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

        miniPhrasesListView.getItems().clear();
        miniPhrasesListView.getItems().addAll(data);
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

        markedPosition = n;
    }

    private void performClear() {
        projecting = false;
        clearScreenButton.setText(clearText);
        clearScreenButton.disableProperty().set(true);
        projectionManager.setText(WrappedText.blankText());
        miniPhrasesListView.getSelectionModel().clearSelection();
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
        data.stream().filter((st) -> (st.selected.get())).forEach((st) -> st.selected.set(false));
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
        
        int[] positions = new int[data.size()];
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
                miniPhrasesListView.scrollTo(st);
                miniPhrasesListView.getSelectionModel().select(st);
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
            ObservableList<String> lines = FXCollections.observableArrayList(text.getLines());
            this.selected = new SimpleBooleanProperty(false);
            this.firstLine = new SimpleBooleanProperty(false);
        }

        private final WrappedText text;
        private final SimpleBooleanProperty selected;
        private final SimpleBooleanProperty firstLine;

        public SimpleBooleanProperty selectedProperty() {
            return selected;
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
