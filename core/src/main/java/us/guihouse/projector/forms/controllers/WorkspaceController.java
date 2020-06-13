/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import us.guihouse.projector.enums.ProjectionListItemType;
import us.guihouse.projector.forms.controllers.projection.BrowserController;
import us.guihouse.projector.forms.controllers.projection.MusicProjectionController;
import us.guihouse.projector.models.ProjectionListItem;
import us.guihouse.projector.models.SimpleProjectionList;
import us.guihouse.projector.other.AwtFontChooseDialog;
import us.guihouse.projector.other.ProjectableItemListCell;
import us.guihouse.projector.other.YouTubeVideoResolve;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrapperFactory;
import us.guihouse.projector.repositories.ProjectionListRepository;
import us.guihouse.projector.scenes.*;
import us.guihouse.projector.services.ManageMusicService;

import static us.guihouse.projector.utils.FilePaths.ALLOWED_WINDOW_CONFIG_FILE_NAME_PATTERN;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class WorkspaceController implements Initializable, SceneObserver, AddMusicCallback, ProjectableItemListCell.CellCallback<ProjectionListItem> {

    private SceneManager sceneManager;
    private GraphicsDeviceHelper graphicsHelper;
    private Stage listMusicStage;
    private Stage statisticsStage;

    private final ManageMusicService manageMusicService = new ManageMusicService();

    private final Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();
    private WrapperFactory wrapperFactory;

    @FXML
    private SplitPane mainPane;

    @FXML
    private Pane loadingPane;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        menuBar.setVisible(false);
        mainPane.setVisible(false);
        loadingPane.setVisible(true);
    }

    public void init(GraphicsDeviceHelper graphicsHelper) {
        this.graphicsHelper = graphicsHelper;

        buildPresetsMenu();
        preparePreview();
        initializeProjectablesList();
        onChangeFullScreen();
        createListMusicStage();

        initProjectionList();
        updateProjectionList();

        graphicsHelper.getWindowConfigsLoader().getConfigFiles().addListener((ListChangeListener<String>) c -> buildPresetsMenu());

        graphicsHelper.getWindowConfigsLoader().loadedConfigFileProperty().addListener((prop, oldValue, newValue) -> updateSelectedPreset(newValue));

        graphicsHelper.getProjectionManager().addTextWrapperChangeListener(factory -> {
            wrapperFactory = factory;
            updateTextWrapper();
        });

        darkenBackgroundMenuItem.setSelected(graphicsHelper.getProjectionManager().getDarkenBackground());
        cropBackgroundMenuItem.setSelected(graphicsHelper.getProjectionManager().getCropBackground());

        menuBar.setVisible(true);
        mainPane.setVisible(true);
        loadingPane.setVisible(false);
    }

    public void stop() {
        graphicsHelper.stop();
    }

    public void onEscapeKeyPressed() {
        projectablesListView
                .getSelectionModel()
                .getSelectedItems()
                .forEach(s -> itemSubScenes.get(s.getId()).onEscapeKeyPressed());
    }

    // ------------------------------
    // Menu
    // ------------------------------
    @FXML
    private MenuBar menuBar;

    @FXML
    private CheckMenuItem cropBackgroundMenuItem;

    @FXML
    private RadioMenuItem singleLineProjectionMenuItem;

    @FXML
    private RadioMenuItem multilineProjectionMenuItem;

    @FXML
    private Menu windowConfigsPresetsMenu;

    @FXML
    private CheckMenuItem fullScreenCheckMenuItem;

    @FXML
    private CheckMenuItem animateBackgroundCheckItem;

    @FXML
    private CheckMenuItem darkenBackgroundMenuItem;

    @FXML
    public void onOpenMusicList() {
        listMusicStage.setX(sceneManager.getStage().getX());
        listMusicStage.setY(sceneManager.getStage().getY());
        listMusicStage.show();
        listMusicStage.requestFocus();
        listMusicStage.setX(sceneManager.getStage().getX());
        listMusicStage.setY(sceneManager.getStage().getY());
    }

    @FXML
    public void onShowStatistics() {
        createStatisticsStage();
        statisticsStage.setX(sceneManager.getStage().getX());
        statisticsStage.setY(sceneManager.getStage().getY());
        statisticsStage.show();
        statisticsStage.setX(sceneManager.getStage().getX());
        statisticsStage.setY(sceneManager.getStage().getY());
    }

    @FXML
    public void onSelectBackgroundImageFile() {
        try {
            Parent changeBg = BgImageScene.createScene(graphicsHelper.getProjectionManager(), getSceneManager());
            getSceneManager().goToParent(changeBg);
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void onDarkenBackgroundChanged() {
        graphicsHelper.getProjectionManager().setDarkenBackground(darkenBackgroundMenuItem.isSelected());
    }

    @FXML
    public void onCropBackgroundChanged() {
        graphicsHelper.getProjectionManager().setCropBackground(cropBackgroundMenuItem.isSelected());
    }

    @FXML
    public void onChangeFont() {
        Font current = graphicsHelper.getProjectionManager().getTextFont();

        AwtFontChooseDialog dialog = new AwtFontChooseDialog(current, (font) -> graphicsHelper.getProjectionManager().setTextFont(font));

        dialog.show();
    }

    @FXML
    public void onChangeFullScreen() {
        graphicsHelper.getProjectionManager().setFullScreen(fullScreenCheckMenuItem.isSelected());
    }

    @FXML
    public void onSingleLineProjection() {
        updateTextWrapper();
    }

    @FXML
    public void onMultilineProjection() {
        updateTextWrapper();
    }

    @FXML
    public void onHelpAbout() {
    }

    @FXML
    public void onHelpManual() {
    }

    @FXML
    public void onReloadScreens() {
        graphicsHelper.reloadDevices();
    }

    @FXML
    public void onCreateWindowConfigPreset() {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("Digitar nome do novo preset");
        inputDialog.setHeaderText("Digite o nome para criar o novo preset (somente letras, numeros, espaços)");
        inputDialog.setContentText("Nome:");

        Optional<String> newValue = inputDialog.showAndWait();

        newValue.ifPresent(value -> {
            if (ALLOWED_WINDOW_CONFIG_FILE_NAME_PATTERN.matcher(value).matches()) {
                if (graphicsHelper.getWindowConfigsLoader().createConfigFileFromDefaults(value + ".json")) {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("OK");
                    a.setHeaderText("Preset criado");
                    a.setContentText("O novo arquivo de preset foi criado");
                    a.show();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Erro");
                    a.setHeaderText("Falha ao criar novo preset");
                    a.setContentText("Verifique se já não existe um com mesmo nome.");
                    a.show();
                }
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erro");
                a.setHeaderText("Falha ao criar novo preset");
                a.setContentText("O nome especificado é inválido.");
                a.show();
            }
        });
    }

    private void updateSelectedPreset(String selected) {
        windowConfigsPresetsMenu.getItems().stream()
                .filter(i -> i instanceof CheckMenuItem)
                .map(i -> (CheckMenuItem)i)
                .forEach(i -> i.setSelected(i.getText().equals(selected)));
    }

    private void buildPresetsMenu() {
        windowConfigsPresetsMenu.getItems().clear();

        graphicsHelper.getWindowConfigsLoader().getConfigFiles().forEach(cf -> {
            CheckMenuItem checkItem = new CheckMenuItem();
            checkItem.setText(cf);
            checkItem.setSelected(cf.equals(graphicsHelper.getWindowConfigsLoader().loadedConfigFileProperty().getValue()));
            checkItem.setOnAction(x -> {
                if (cf.equals(graphicsHelper.getWindowConfigsLoader().loadedConfigFileProperty().getValue())) {
                    graphicsHelper.getWindowConfigsLoader().loadDefaultConfigs();
                } else {
                    graphicsHelper.getWindowConfigsLoader().loadConfigs(cf);
                }
            });
            windowConfigsPresetsMenu.getItems().add(checkItem);
        });
    }

    // ------------------------------
    // Projection List
    // ------------------------------
    private final ProjectionListRepository listRepository = new ProjectionListRepository();

    @FXML
    private ChoiceBox<SimpleProjectionList> projectionListChoice;

    @FXML
    private MenuButton projectionListOptionsMenuButton;

    private void initProjectionList() {
        projectionListChoice.getItems().clear();
        projectionListChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleProjectionList object) {
                return object.getTitle();
            }

            @Override
            public SimpleProjectionList fromString(String string) {
                return projectionListChoice.getItems().stream().filter(i -> i.getTitle().equals(string)).findAny().orElse(null);
            }
        });

        projectionListChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                projectablesListView.setVisible(false);
                addItemButtonGroup.setVisible(false);
                projectionListOptionsMenuButton.setVisible(false);
            } else {
                if (newValue.getTitle().equals("<Criar nova lista>")) {
                    TextInputDialog inputDialog = new TextInputDialog();
                    inputDialog.setTitle("Digitar Título");
                    inputDialog.setHeaderText("Digite o título da nova lista de projeção");
                    inputDialog.setContentText("Título:");

                    Optional<String> titleOp = inputDialog.showAndWait();
                    if (titleOp.isPresent()) {
                        try {
                            SimpleProjectionList list = listRepository.createList(titleOp.get());
                            updateProjectionList();
                            projectionListChoice.getSelectionModel().select(list);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        projectionListChoice.getSelectionModel().select(oldValue);
                    }

                } else {
                    setProjectablesList(newValue);
                    projectablesListView.setVisible(true);
                    addItemButtonGroup.setVisible(true);
                    projectionListOptionsMenuButton.setVisible(true);
                }
            }
        });
    }

    private void updateProjectionList() {
        projectionListChoice.getItems().clear();

        SimpleProjectionList createNewItem = new SimpleProjectionList();
        createNewItem.setTitle("<Criar nova lista>");
        projectionListChoice.getItems().add(createNewItem);

        try {
            projectionListChoice.getItems().addAll(listRepository.activeLists());
        } catch (SQLException e) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @FXML
    public void onDeleteProjectionList() {
        try {
            listRepository.deleteList(projectionListChoice.getSelectionModel().getSelectedItem());
            updateProjectionList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------
    // Current Projection List
    // ------------------------------
    @FXML
    private ListView<ProjectionListItem> projectablesListView;

    @FXML
    private Pane targetPane;

    @FXML
    private MenuButton addItemButtonGroup;

    private SimpleProjectionList projectionList;

    private Map<Long, ProjectionItemSubScene> itemSubScenes;

    @Override
    public void onDragDone(List<ProjectionListItem> items) {
        int sort = 0;

        for (ProjectionListItem item : items) {
            item.setOrder(sort);
            sort++;
            try {
                listRepository.updateItemSort(item);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    class ProjectablesListViewCellFactory implements Callback<ListView<ProjectionListItem>, ListCell<ProjectionListItem>> {
        @Override
        public ListCell<ProjectionListItem> call(ListView<ProjectionListItem> arg) {
            return new ProjectableItemListCell(WorkspaceController.this);
        }
    }

    private void initializeProjectablesList() {
        itemSubScenes = new HashMap<>();

        projectablesListView.setVisible(false);
        addItemButtonGroup.setVisible(false);
        projectionListOptionsMenuButton.setVisible(false);

        projectablesListView.setCellFactory(new ProjectablesListViewCellFactory());

        projectablesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        projectablesListView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int val = newValue.intValue();
                if (val >= 0 && val < projectablesListView.getItems().size()) {
                    setProjectionView(projectablesListView.getItems().get(val));
                    return;
                }
            }

            setProjectionView(null);
        });

        projectablesListView.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.DELETE) {
                try {
                    listRepository.deleteItem(projectablesListView.getSelectionModel().getSelectedItem());
                    Platform.runLater(this::reloadProjectables);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setProjectablesList(SimpleProjectionList simpleProjectablesList) {
        projectionList = simpleProjectablesList;
        itemSubScenes.clear();

        reloadProjectables();
    }

    private void reloadProjectables() {
        projectablesListView.getItems().clear();

        try {
            List<ProjectionListItem> list = listRepository.getItems(projectionList.getId());

            projectablesListView.getItems().addAll(list);

            for (ProjectionListItem item : list) {
                if (itemSubScenes.get(item.getId()) == null) {
                    createComponent(item);
                }
            }

            for (Map.Entry<Long, ProjectionItemSubScene> entry : itemSubScenes.entrySet()) {
                if (list.stream().noneMatch(item -> item.getId() == entry.getKey())) {
                    ProjectionItemSubScene scene = itemSubScenes.remove(entry.getKey());
                    scene.stop();
                }
            }
        } catch (SQLException e) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void createComponent(ProjectionListItem item) {
        try {
            ProjectionItemSubScene created = item.getType().createSubScene(targetPane.getWidth(), targetPane.getHeight());
            if (created != null) {
                itemSubScenes.put(item.getId(), created);

                created.setObserver(this);
                created.setSceneManager(getSceneManager());
                created.setProjectionListItem(item);
                created.setProjectionListRepository(listRepository);

                if (created instanceof MusicProjectionScene) {
                    MusicProjectionScene scene = (MusicProjectionScene) created;
                    scene.getTextWrapperProperty().bind(textWrapperProperty);
                    scene.setManageMusicService(manageMusicService);
                }

                created.initWithProjectionManager(graphicsHelper.getProjectionManager());
                created.widthProperty().bind(targetPane.widthProperty());
                created.heightProperty().bind(targetPane.heightProperty());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setProjectionView(ProjectionListItem item) {
        if (item == null) {
            targetPane.getChildren().clear();
            return;
        }

        if (targetPane.getChildren().size() <= 0) {
            targetPane.getChildren().add(itemSubScenes.get(item.getId()));
            return;
        }

        if (Objects.equals(targetPane.getChildren().get(0), itemSubScenes.get(item.getId()))) {
            return;
        }

        targetPane.getChildren().clear();
        targetPane.getChildren().add(itemSubScenes.get(item.getId()));
    }

    @FXML
    public void onAddMusic() {
        listMusicStage.setX(sceneManager.getStage().getX());
        listMusicStage.setY(sceneManager.getStage().getY());
        listMusicStage.show();
        listMusicStage.requestFocus();
        listMusicStage.setX(sceneManager.getStage().getX());
        listMusicStage.setY(sceneManager.getStage().getY());
    }

    @FXML
    public void onAddYouTube() {
        YouTubeVideoResolve.getVideoEmbedUrl((event) -> {
            if (event.isError()) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erro");
                a.setHeaderText("Falha ao obter dados do vídeo");
                a.setContentText("Não foi possível obter a URL 'Embed'");
                a.show();
            } else if (event.getResolved() != null) {
                addBrowser(event.getResolved());
            }
        });
    }

    @FXML
    public void onAddBrowser() {
        addBrowser("https://google.com.br");
    }

    private void addBrowser(String url) {
        try {
            ProjectionListItem item = listRepository.createItem(projectionList, "Navegador", ProjectionListItemType.WEB_SITE);
            HashMap<String, String> websiteProps = new HashMap<>();
            websiteProps.put(BrowserController.URL_PROPERTY, url);
            listRepository.updateItemProperties(item.getId(), websiteProps);
            reloadProjectables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onAddPicture() {
        try {
            listRepository.createItem(projectionList, "Álbum de Imagens", ProjectionListItemType.IMAGE);
            reloadProjectables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onAddText() {
        try {
            listRepository.createItem(projectionList, "Texto", ProjectionListItemType.TEXT);
            reloadProjectables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onAddPlayer() {
        try {
            listRepository.createItem(projectionList, "Player", ProjectionListItemType.VIDEO);
            reloadProjectables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean addMusic(Integer id) {
        for (ProjectionItemSubScene i : itemSubScenes.values()) {
            if (i instanceof MusicProjectionScene) {
                MusicProjectionScene mps = (MusicProjectionScene) i;
                if (mps.getMusicId() == id) {
                    return false;
                }
            }
        }

        if (projectionList == null) {
            return true;
        }

        try {
            ProjectionListItem item = listRepository.createItem(projectionList, "Musica", ProjectionListItemType.MUSIC);
            HashMap<String, String> musicProps = new HashMap<>();
            musicProps.put(MusicProjectionController.MUSIC_ID_PROPERTY, Integer.toString(id));
            listRepository.updateItemProperties(item.getId(), musicProps);
            reloadProjectables();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    // ------------------------------
    // Preview
    // ------------------------------
    @FXML
    private TitledPane previewPane;

    private void preparePreview() {
        StackPane background = new StackPane();
        background.setStyle("-fx-background-color: BLACK");
        background.getChildren().add(graphicsHelper.getPreviewPanel());
        previewPane.setContent(background);
        graphicsHelper.getPreviewPanel().fitWidthProperty().bind(previewPane.widthProperty());
        graphicsHelper.getPreviewPanel().fitHeightProperty().bind(previewPane.heightProperty());
    }

    // ------------------------------
    // Other
    // ------------------------------
    @Override
    public void titleChanged(ProjectionListItem item) {
        projectablesListView.getItems().forEach(listItem -> {
            if (listItem.getId() == item.getId()) {
                listItem.setTitle(item.getTitle());
                projectablesListView.refresh();
            }
        });
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private void createListMusicStage() {
        try {
            listMusicStage = new Stage();
            Parent list = MusicListScene.createMusicListScene(this, manageMusicService, listMusicStage);
            Scene listScene = new Scene(list, 800, 480);
            listMusicStage.setScene(listScene);
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createStatisticsStage() {
        try {
            statisticsStage = new Stage();
            Parent list = StatisticsScene.createStatisticsScene(manageMusicService, statisticsStage);
            Scene listScene = new Scene(list, 800, 480);
            statisticsStage.setScene(listScene);
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateTextWrapper() {
        if (singleLineProjectionMenuItem.isSelected()) {
            textWrapperProperty.setValue(wrapperFactory.getTextWrapper(false));
        } else if (multilineProjectionMenuItem.isSelected()) {
            textWrapperProperty.setValue(wrapperFactory.getTextWrapper(true));
        }
    }
}
