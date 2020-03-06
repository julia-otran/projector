/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import us.guihouse.projector.forms.controllers.projection.ProjectionController;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.models.BackgroundModel;
import us.guihouse.projector.services.ImageDragDropService;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class BgImageController extends ProjectionController {
    private BackgroundModel backgroundModel;

    private ImageDragDropService backgroundDragDropService;
    private ImageDragDropService logoDragDropService;
    private ImageDragDropService overlayDragDropService;

    @FXML
    private RadioButton withoutBackgroundRadio;

    @FXML
    private RadioButton staticRadio;

    @FXML
    private RadioButton animationRadio;

    @FXML
    private VBox backgroundBox;

    @FXML
    private VBox logoBox;

    @FXML
    private VBox overlayBox;

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private ImageView logoImageView;

    @FXML
    private ImageView overlayImageView;

    @FXML
    private Pane backgroundImagePane;

    @FXML
    private Pane logoImagePane;

    @FXML
    private Pane overlayImagePane;

    @FXML
    private Label backgroundDragDropLabel;

    @FXML
    private Label logoDragDropLabel;

    @FXML
    private Label overlayDragDropLabel;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initImageView(backgroundImageView, backgroundImagePane);
        initImageView(logoImageView, logoImagePane);
        initImageView(overlayImageView, overlayImagePane);

        withoutBackgroundRadio.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    backgroundBox.setVisible(false);
                    logoBox.setVisible(false);
                    overlayBox.setVisible(false);
                    backgroundModel.setType(BackgroundModel.Type.NONE);
                    loadImagesByType();
                    dispatchChanged();
                }
            }
        });

        staticRadio.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    backgroundBox.setVisible(true);
                    logoBox.setVisible(false);
                    overlayBox.setVisible(false);
                    backgroundModel.setType(BackgroundModel.Type.STATIC);
                    loadImagesByType();
                    dispatchChanged();
                }
            }
        });

        animationRadio.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    backgroundBox.setVisible(true);
                    logoBox.setVisible(true);
                    overlayBox.setVisible(true);
                    backgroundModel.setType(BackgroundModel.Type.OVERLAY_ANIMATED);
                    loadImagesByType();
                    dispatchChanged();
                }
            }
        });
    }

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        backgroundModel = getProjectionManager().getBackgroundModel();
        load();

        setupBackgroundDragDrop();
        setupLogoDragDrop();
        setupOverlayDragDrop();
    }

    private void load() {
        if (backgroundModel.getType() == BackgroundModel.Type.NONE) {
            withoutBackgroundRadio.fire();
        }

        if (backgroundModel.getType() == BackgroundModel.Type.STATIC) {
            staticRadio.fire();
        }

        if (backgroundModel.getType() == BackgroundModel.Type.OVERLAY_ANIMATED) {
            animationRadio.fire();
        }
    }

    private void loadImagesByType() {
        if (staticRadio.isSelected()) {
            if (backgroundModel.getStaticBackgroundFile().canRead()) {
                Image background = new Image(backgroundModel.getStaticBackgroundFile().toURI().toString());
                backgroundImageView.setImage(background);
            }
        }

        if (animationRadio.isSelected()) {
            if (backgroundModel.getBackgroundFile() != null && backgroundModel.getBackgroundFile().canRead()) {
                Image background = new Image(backgroundModel.getBackgroundFile().toURI().toString());
                backgroundImageView.setImage(background);
            } else {
                backgroundImageView.setImage(null);
            }

            if (backgroundModel.getLogoFile() != null && backgroundModel.getLogoFile().canRead()) {
                Image logo = new Image(backgroundModel.getLogoFile().toURI().toString());
                logoImageView.setImage(logo);
            } else {
                logoImageView.setImage(null);
            }

            if (backgroundModel.getOverlayFile() != null && backgroundModel.getOverlayFile().canRead()) {
                Image overlay = new Image(backgroundModel.getOverlayFile().toURI().toString());
                overlayImageView.setImage(overlay);
            } else {
                overlayImageView.setImage(null);
            }
        }
    }

    private void initImageView(ImageView imageView, Pane container) {
        imageView.fitWidthProperty().bind(container.widthProperty());
        imageView.fitHeightProperty().bind(container.heightProperty());
    }

    private void setupBackgroundDragDrop() {
        String oldText = backgroundDragDropLabel.getText();

        backgroundBox.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                backgroundDragDropService.onDragExit();
            }
        });

        backgroundBox.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                backgroundDragDropService.onDragDropped(event);
            }
        });

        backgroundBox.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                backgroundDragDropService.onDragOver(event);
            }
        });

        ImageDragDropService.Client client = new ImageDragDropService.Client() {

            @Override
            public void onFileLoading() {
                backgroundBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                backgroundDragDropLabel.setText("Aguarde, carregando...");
            }

            @Override
            public void onFileOk() {
                backgroundBox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                backgroundDragDropLabel.setText("Soltar na área demarcada");
            }

            @Override
            public void onFileError(String message) {
                backgroundBox.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                backgroundDragDropLabel.setText(message);
            }

            @Override
            public void onDropSuccess(Image image, File imageFile) {
                backgroundBox.setBorder(null);

                if (BackgroundModel.Type.STATIC.equals(backgroundModel.getType())) {
                    backgroundModel.setStaticBackgroundFile(imageFile);
                } else {
                    backgroundModel.setBackgroundFile(imageFile);
                }

                backgroundImageView.setImage(image);

                dispatchChanged();
            }

            @Override
            public void onDropAbort() {
                backgroundBox.setBorder(null);
                File imageFile;

                if (BackgroundModel.Type.STATIC.equals(backgroundModel.getType())) {
                    imageFile = backgroundModel.getStaticBackgroundFile();
                } else {
                    imageFile = backgroundModel.getBackgroundFile();
                }

                if (imageFile != null) {
                    backgroundImageView.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    backgroundImageView.setImage(null);
                }

                backgroundDragDropLabel.setText(oldText);
            }

            @Override
            public void showPreviewImage(Image image) {
                backgroundImageView.setImage(image);
            }
        };

        backgroundDragDropService = new ImageDragDropService(client, true);
    }

    private void setupLogoDragDrop() {
        String oldText = logoDragDropLabel.getText();

        ImageDragDropService.Client client = new ImageDragDropService.Client() {

            @Override
            public void onFileLoading() {
                logoBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                logoDragDropLabel.setText("Aguarde, carregando...");
            }

            @Override
            public void onFileOk() {
                logoBox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                logoDragDropLabel.setText("Soltar na área demarcada");
            }

            @Override
            public void onFileError(String message) {
                logoBox.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                logoDragDropLabel.setText(message);
            }

            @Override
            public void onDropSuccess(Image image, File imageFile) {
                logoBox.setBorder(null);
                backgroundModel.setLogoFile(imageFile);

                logoImageView.setImage(image);

                dispatchChanged();
            }

            @Override
            public void onDropAbort() {
                logoBox.setBorder(null);

                if (backgroundModel.getLogoFile() != null) {
                    logoImageView.setImage(new Image(backgroundModel.getLogoFile().toURI().toString()));
                } else {
                    logoImageView.setImage(null);
                }

                logoDragDropLabel.setText(oldText);
            }

            @Override
            public void showPreviewImage(Image image) {
                logoImageView.setImage(image);
            }
        };

        logoDragDropService = new ImageDragDropService(client, true);

        logoBox.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logoDragDropService.onDragExit();
            }
        });

        logoBox.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logoDragDropService.onDragDropped(event);
            }
        });

        logoBox.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                logoDragDropService.onDragOver(event);
            }
        });
    }

    private void setupOverlayDragDrop() {
        String oldText = overlayDragDropLabel.getText();

        ImageDragDropService.Client client = new ImageDragDropService.Client() {

            @Override
            public void onFileLoading() {
                overlayBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                overlayDragDropLabel.setText("Aguarde, carregando...");
            }

            @Override
            public void onFileOk() {
                overlayBox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                overlayDragDropLabel.setText("Soltar na área demarcada");
            }

            @Override
            public void onFileError(String message) {
                overlayBox.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
                overlayDragDropLabel.setText(message);
            }

            @Override
            public void onDropSuccess(Image image, File imageFile) {
                overlayBox.setBorder(null);
                backgroundModel.setOverlayFile(imageFile);

                overlayImageView.setImage(image);

                dispatchChanged();
            }

            @Override
            public void onDropAbort() {
                overlayBox.setBorder(null);

                if (backgroundModel.getOverlayFile() != null) {
                    overlayImageView.setImage(new Image(backgroundModel.getOverlayFile().toURI().toString()));
                } else {
                    overlayImageView.setImage(null);
                }

                overlayDragDropLabel.setText(oldText);
            }

            @Override
            public void showPreviewImage(Image image) {
                overlayImageView.setImage(image);
            }
        };

        overlayDragDropService = new ImageDragDropService(client, true);

        overlayBox.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                overlayDragDropService.onDragExit();
            }
        });

        overlayBox.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                overlayDragDropService.onDragDropped(event);
            }
        });

        overlayBox.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                overlayDragDropService.onDragOver(event);
            }
        });
    }

    private void dispatchChanged() {
        getProjectionManager().setBackgroundModel(backgroundModel);
    }

    @FXML
    public void onCancel() {
        getSceneManager().goToWorkspace();
    }
    
    @Override
    public void onEscapeKeyPressed() {
        getSceneManager().goToWorkspace();
    }

    @Override
    public void stop() {

    }

    @FXML
    public void withoutBackgroundClick() {
        backgroundImageView.setImage(null);
        if (BackgroundModel.Type.STATIC.equals(backgroundModel.getType())) {
            backgroundModel.setStaticBackgroundFile(null);
        } else {
            backgroundModel.setBackgroundFile(null);
        }
        dispatchChanged();
    }

    @FXML
    public void withoutLogoClick() {
        logoImageView.setImage(null);
        backgroundModel.setLogoFile(null);
        dispatchChanged();
    }

    @FXML
    public void withoutOverlayClick() {
        overlayImageView.setImage(null);
        backgroundModel.setOverlayFile(null);
        dispatchChanged();
    }
}
