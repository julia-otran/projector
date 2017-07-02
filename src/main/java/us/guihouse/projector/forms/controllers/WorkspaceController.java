/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import us.guihouse.projector.other.ListCellX;
import us.guihouse.projector.other.YouTubeVideoResolve;
import us.guihouse.projector.projection.TextWrapperFactoryChangeListener;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrapperFactory;
import us.guihouse.projector.scenes.BgImageScene;
import us.guihouse.projector.scenes.BrowserSubScene;
import us.guihouse.projector.scenes.ImageSubScene;
import us.guihouse.projector.scenes.MusicListScene;
import us.guihouse.projector.scenes.MusicProjectionScene;
import us.guihouse.projector.scenes.ProjectionItemSubScene;
import us.guihouse.projector.scenes.SceneObserver;
import us.guihouse.projector.scenes.TextSubScene;
import us.guihouse.projector.services.ManageMusicService;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class WorkspaceController implements Initializable, SceneObserver, AddMusicCallback {

    private SceneManager sceneManager;
    private GraphicsDeviceHelper graphicsHelper;
    private Stage listMusicStage;
    private final ManageMusicService manageMusicService = new ManageMusicService();

    private Property<TextWrapper> textWrapperProperty = new SimpleObjectProperty<>();
    private WrapperFactory wrapperFactory;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graphicsHelper = new GraphicsDeviceHelper(projectionScreenMenu);
        initializeProjectionList();
        onCropBackgroundChanged();
        onChangeFullScreen();
        createListMusicStage();

        graphicsHelper.getProjectionManager().addTextWrapperChangeListener(new TextWrapperFactoryChangeListener() {
            @Override
            public void onWrapperFactoryChanged(WrapperFactory factory) {
                wrapperFactory = factory;
                updateTextWrapper();
            }
        });
    }

    public void stop() {
        graphicsHelper.stop();
    }

    public void dispose() {
        graphicsHelper.dispose();
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
    public void onOpenMusicList() {
        listMusicStage.show();
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
    public void onRemoveBackground() {
        graphicsHelper.getProjectionManager().setBackgroundImageFile(null);
    }

    @FXML
    public void onCropBackgroundChanged() {
        graphicsHelper.getProjectionManager().setCropBackground(cropBackgroundMenuItem.isSelected());
    }

    @FXML
    public void onChangeFont() {

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
    @FXML
    private ListView<ProjectionItemSubScene> projectionListView;

    private boolean changingTitle = false;

    private void initializeProjectionList() {
        projectionListView.setCellFactory(new Callback<ListView<ProjectionItemSubScene>, ListCell<ProjectionItemSubScene>>() {
            @Override
            public ListCell<ProjectionItemSubScene> call(ListView<ProjectionItemSubScene> arg0) {
                //My cell is on my way to call
                ListCellX<ProjectionItemSubScene> cell = new ListCellX<ProjectionItemSubScene>() {
                    @Override
                    public void updateItem(ProjectionItemSubScene item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            //finally every thing is just setup
                            setText(item.getTitle());
                        }
                    }
                };
                //this is my chemical solution
                //Need to call on every cell for making things work
                cell.init(projectionListView.getItems());

                //Take my cell
                return cell;
            }

        });

        projectionListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        projectionListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (changingTitle) {
                    return;
                }

                if (newValue != null) {
                    int val = newValue.intValue();
                    if (val >= 0 && val < projectionListView.getItems().size()) {
                        setProjectionView(projectionListView.getItems().get(val));
                        return;
                    }
                }

                setProjectionView(null);
            }
        });
    }

    private void setProjectionView(ProjectionItemSubScene scene) {
        if (scene == null) {
            targetPane.getChildren().clear();
            return;
        }

        if (targetPane.getChildren().size() <= 0) {
            targetPane.getChildren().add(scene);
            return;
        }

        if (Objects.equals(targetPane.getChildren().get(0), scene)) {
            return;
        }

        targetPane.getChildren().clear();
        targetPane.getChildren().add(scene);
    }

    @FXML
    public void onAddMusic() {
        listMusicStage.show();
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
                addBroser(event.getResolved());
            }
        });
    }

    @FXML
    public void onAddBrowser() {
        addBroser("https://google.com.br");
    }

    private void addBroser(String url) {
        try {
            BrowserSubScene created = BrowserSubScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);
            created.setUrl(url);

            changingTitle = true;
            projectionListView.getItems().add(created);
            changingTitle = false;

            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void onAddPicture() {
        try {
            ProjectionItemSubScene created = ImageSubScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);

            projectionListView.getItems().add(created);
            projectionListView.getSelectionModel().select(projectionListView.getItems().size() - 1);

            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void onAddText() {
        try {
            ProjectionItemSubScene created = TextSubScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);

            projectionListView.getItems().add(created);
            projectionListView.getSelectionModel().select(projectionListView.getItems().size() - 1);

            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // ------------------------------
    // Preview
    // ------------------------------
    @FXML
    private Canvas previewCanvas;

    @FXML
    private Pane targetPane;

    @Override
    public void titleChanged() {
        projectionListView.refresh();
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private void createListMusicStage() {
        try {
            Parent list = MusicListScene.createMusicListScene(this, manageMusicService);
            Scene listScene = new Scene(list, 800, 480);
            listMusicStage = new Stage();
            listMusicStage.setScene(listScene);
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean addMusic(Integer id) {
        for (ProjectionItemSubScene i : projectionListView.getItems()) {
            if (i instanceof MusicProjectionScene) {
                MusicProjectionScene mps = (MusicProjectionScene) i;
                if (mps.getMusicId() == id) {
                    return false;
                }
            }
        };

        try {
            MusicProjectionScene created = MusicProjectionScene.createScene(targetPane.getWidth(), targetPane.getHeight());
            created.setObserver(this);
            created.setManageMusicService(manageMusicService);
            created.loadMusicWithId(id);
            created.getTextWrapperProperty().bind(textWrapperProperty);

            projectionListView.getItems().add(created);
            projectionListView.getSelectionModel().select(projectionListView.getItems().size() - 1);

            created.initWithProjectionManager(graphicsHelper.getProjectionManager());
            created.widthProperty().bind(targetPane.widthProperty());
            created.heightProperty().bind(targetPane.heightProperty());
        } catch (IOException ex) {
            Logger.getLogger(WorkspaceController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    private void updateTextWrapper() {
        if (singleLineProjectionMenuItem.isSelected()) {
            textWrapperProperty.setValue(wrapperFactory.getTextWrapper(false));
        } else if (multilineProjectionMenuItem.isSelected()) {
            textWrapperProperty.setValue(wrapperFactory.getTextWrapper(true));
        }
    }
}
