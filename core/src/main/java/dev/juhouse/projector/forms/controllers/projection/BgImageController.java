/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import dev.juhouse.projector.projection.ProjectionManager;
import dev.juhouse.projector.projection.models.BackgroundModel;
import dev.juhouse.projector.services.ImageDragDropService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class BgImageController extends ProjectionController {
    private BackgroundModel backgroundModel;

    private ImageDragDropService backgroundDragDropService;

    @FXML
    private RadioButton withoutBackgroundRadio;

    @FXML
    private RadioButton staticRadio;

    @FXML
    private VBox backgroundBox;

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private Pane backgroundImagePane;

    @FXML
    private Label backgroundDragDropLabel;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initImageView(backgroundImageView, backgroundImagePane);

        withoutBackgroundRadio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                backgroundBox.setVisible(false);
                backgroundModel.setStaticBackgroundFile(null);
                loadImagesByType();
                dispatchChanged();
            }
        });

        staticRadio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                backgroundBox.setVisible(true);
                loadImagesByType();
                dispatchChanged();
            }
        });
    }

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        backgroundModel = getProjectionManager().getBackgroundModel();
        load();

        setupBackgroundDragDrop();
    }

    private void load() {
        if (backgroundModel.getStaticBackground() == null) {
            withoutBackgroundRadio.fire();
        } else {
            staticRadio.fire();
        }
    }

    private void loadImagesByType() {
        if (staticRadio.isSelected()) {
            if (backgroundModel.getStaticBackgroundFile() != null && backgroundModel.getStaticBackgroundFile().canRead()) {
                Image background = new Image(backgroundModel.getStaticBackgroundFile().toURI().toString());
                backgroundImageView.setImage(background);
            } else {
                backgroundImageView.setImage(null);
            }
        }
    }

    private void initImageView(ImageView imageView, Pane container) {
        imageView.fitWidthProperty().bind(container.widthProperty());
        imageView.fitHeightProperty().bind(container.heightProperty());
    }

    private void setupBackgroundDragDrop() {
        String oldText = backgroundDragDropLabel.getText();

        backgroundBox.setOnDragExited(event -> backgroundDragDropService.onDragExit());

        backgroundBox.setOnDragDropped(event -> backgroundDragDropService.onDragDropped(event));

        backgroundBox.setOnDragOver(event -> backgroundDragDropService.onDragOver(event));

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
                backgroundModel.setStaticBackgroundFile(imageFile);
                backgroundImageView.setImage(image);

                dispatchChanged();
            }

            @Override
            public void onDropAbort() {
                backgroundBox.setBorder(null);
                File imageFile;

                imageFile = backgroundModel.getStaticBackgroundFile();

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
        backgroundModel.setStaticBackgroundFile(null);
        dispatchChanged();
    }
}
