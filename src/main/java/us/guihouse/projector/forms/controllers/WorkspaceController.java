/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.awt.Font;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import sun.awt.X11.XKeyEvent;
import us.guihouse.projector.enums.ProjectionListItemType;
import us.guihouse.projector.models.ProjectionListItem;
import us.guihouse.projector.models.SimpleProjectionList;
import us.guihouse.projector.other.AwtFontChooseDialog;
import us.guihouse.projector.other.ProjectableItemListCell;
import us.guihouse.projector.other.YouTubeVideoResolve;
import us.guihouse.projector.projection.TextWrapperFactoryChangeListener;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrapperFactory;
import us.guihouse.projector.repositories.ProjectionListRepository;
import us.guihouse.projector.scenes.*;
import us.guihouse.projector.services.ManageMusicService;

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

    private Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();
    private WrapperFactory wrapperFactory;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graphicsHelper = new GraphicsDeviceHelper(projectionScreenMenu);

        preparePreview();
        initializeProjectablesList();
        onCropBackgroundChanged();
        onChangeFullScreen();
        createListMusicStage();

        initProjectionList();
        updateProjectionList();

        graphicsHelper.getProjectionManager().addTextWrapperChangeListener(new TextWrapperFactoryChangeListener() {
            @Override
            public void onWrapperFactoryChanged(WrapperFactory factory) {
                wrapperFactory = factory;
                updateTextWrapper();
            }
        });

        darkenBackgroundMenuItem.setSelected(graphicsHelper.getProjectionManager().getDarkenBackground());
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
    private CheckMenuItem cropBackgroundMenuItem;

    @FXML
    private RadioMenuItem singleLineProjectionMenuItem;

    @FXML
    private RadioMenuItem multilineProjectionMenuItem;

    @FXML
    private Menu projectionScreenMenu;

    @FXML
    private CheckMenuItem fullScreenCheckMenuItem;

    @FXML
    private CheckMenuItem animateBackgroundCheckItem;

    @FXML
    private CheckMenuItem darkenBackgroundMenuItem;

    @FXML
    public void onOpenMusicList() {
        listMusicStage.show();
    }

    @FXML
    public void onShowStatistics() {
        createStatisticsStage();
        statisticsStage.show();
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
    public void onAnimateBackgroundChanged() {
        graphicsHelper.getProjectionManager().setAnimateBackground(animateBackgroundCheckItem.isSelected());
    }

    @FXML
    public void onChangeFont() {
        Font current = graphicsHelper.getProjectionManager().getTextFont();

        AwtFontChooseDialog dialog = new AwtFontChooseDialog(current, (font) -> {
            graphicsHelper.getProjectionManager().setTextFont(font);
        });

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

    // ------------------------------
    // Projection List
    // ------------------------------
    private ProjectionListRepository listRepository = new ProjectionListRepository();

    @FXML
    private ChoiceBox<SimpleProjectionList> projectionListChoice;

    private void initProjectionList() {
        projectionListChoice.setConverter(new StringConverter<SimpleProjectionList>() {
            @Override
            public String toString(SimpleProjectionList object) {
                return object.getTitle();
            }

            @Override
            public SimpleProjectionList fromString(String string) {
                return projectionListChoice.getItems().stream().filter(i -> i.getTitle().equals(string)).findAny().orElse(null);
            }
        });

        projectionListChoice.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SimpleProjectionList>() {
            @Override
            public void changed(ObservableValue<? extends SimpleProjectionList> observable, SimpleProjectionList oldValue, SimpleProjectionList newValue) {
                if (newValue != null) {
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
                    }
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

        projectablesListView.setCellFactory(new ProjectablesListViewCellFactory());

        projectablesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        projectablesListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (newValue != null) {
                    int val = newValue.intValue();
                    if (val >= 0 && val < projectablesListView.getItems().size()) {
                        setProjectionView(projectablesListView.getItems().get(val));
                        return;
                    }
                }

                setProjectionView(null);
            }
        });

        projectablesListView.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.DELETE) {
                try {
                    listRepository.deleteItem(projectablesListView.getSelectionModel().getSelectedItem());
                    reloadProjectables();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setProjectablesList(SimpleProjectionList simpleProjectablesList) {
        projectablesListView.setVisible(true);
        addItemButtonGroup.setVisible(true);
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
        listMusicStage.show();
        listMusicStage.requestFocus();
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
        };

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
    private SwingNode previewNode;

    private void preparePreview() {
        previewNode = new SwingNode();
        previewPane.setContent(previewNode);
        previewNode.setContent(graphicsHelper.getPreviewPanel());
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
