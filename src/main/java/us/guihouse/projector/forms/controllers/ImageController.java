/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import us.guihouse.projector.projection.ProjectionImage;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.services.ImageDragDropService;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class ImageController extends ProjectionController implements ImageDragDropService.Client, Runnable {
    private ImageDragDropService service;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        service = new ImageDragDropService(this);

        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(true);
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());

        imageView.fitWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                centerImage();
            }
        });

        imageView.fitHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                centerImage();
            }
        }
                );
    }

    @FXML
    private Button beginProjectionButton;

    @FXML
    private Button endProjectionButton;

    @FXML
    private Label dragDropLabel;

    @FXML
    private Pane imagePane;

    @FXML
    private ImageView imageView;

    private boolean preview = false;
    private boolean running = false;
    private int projectingIndex;
    private List<Image> images = new ArrayList<>();

    @FXML
    private ScrollPane imagesList;

    private ProjectionImage projectable;

    private String oldLabelText;

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        this.projectable = projectionManager.createImage();
        projectable.setCropBackground(false);
        oldLabelText = dragDropLabel.getText();
        this.preview = false;
    }

    @FXML
    public void onDragOver(DragEvent event) {
        service.onDragOver(event);
    }

    @FXML
    public void onDragExit() {
        service.onDragExit();
    }

    @FXML
    public void onDragDropped(DragEvent event) {
        service.onDragDropped(event);
    }

    private void setError(String error) {
        dragDropLabel.setVisible(true);
        dragDropLabel.setText(error);
        imagePane.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }

    private void setOriginal() {
        dragDropLabel.setText(oldLabelText);
        imagePane.setBorder(null);
        preview = false;
        if (projectingIndex >= 0) {
            imageView.setImage(images.get(projectingIndex));
        } else {
            imageView.setImage(null);
        }
    }

    @FXML
    public void onBeginProjection() {
        Image currentImage;

        if (projectingIndex >= 0) {
            currentImage = images.get(projectingIndex);
        } else {
            getProjectionManager().setProjectable(null);
            return;
        }

        projectable.setImage(SwingFXUtils.fromFXImage(currentImage, null));
        getProjectionManager().setProjectable(projectable);
        imageView.setImage(currentImage);

        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(false);
        start();
    }

    @FXML
    public void onEndProjection() {
        getProjectionManager().setProjectable(null);
        beginProjectionButton.disableProperty().set(false);
        endProjectionButton.disableProperty().set(true);
        stop();
    }

    public void start() {
        running = true;
        Platform.runLater(this);
    }

    public void stop() {
        running = false;
    }

    private long time;
    @Override
    public void run() {
        if (!running) {
            return;
        }

        start();

        long current = System.currentTimeMillis();

        if (current - time < 2000) {
            return;
        }

        time = current;

        projectingIndex++;
        if (projectingIndex >= images.size()) {
            projectingIndex = 0;
        }

        Image proj = images.get(projectingIndex);
        projectable.setImage(SwingFXUtils.fromFXImage(proj, null));

        if (!preview) {
            imageView.setImage(proj);
        }


    }

    private void centerImage() {
        Image img = imageView.getImage();
        if (img != null) {
            double w = 0;
            double h = 0;

            double ratioX = imageView.getFitWidth() / img.getWidth();
            double ratioY = imageView.getFitHeight() / img.getHeight();

            double reducCoeff = 0;
            if(ratioX >= ratioY) {
                reducCoeff = ratioY;
            } else {
                reducCoeff = ratioX;
            }

            w = img.getWidth() * reducCoeff;
            h = img.getHeight() * reducCoeff;

            imageView.setX((imageView.getFitWidth() - w) / 2);
            imageView.setY((imageView.getFitHeight() - h) / 2);
        }
    }

    @Override
    public void onFileLoading() {
        dragDropLabel.setText("Aguarde, lendo imagem...");
        imagePane.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }

    @Override
    public void onFileOk() {
        dragDropLabel.setText("Solte na área demarcada");
        imagePane.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }

    @Override
    public void onFileError(String message) {
        setError(message);
    }

    @Override
    public void onDropSuccess(Image image, File file) {
        beginProjectionButton.disableProperty().set(false);
        images.add(image);
        if (file != null && !file.getName().isEmpty()) {
            notifyTitleChange(file.getName());
        }
        setOriginal();
    }

    @Override
    public void onDropAbort() {
        setOriginal();
    }

    @Override
    public void showPreviewImage(Image image) {
        imageView.setImage(image);
        preview = true;
        centerImage();
    }

    @Override
    public void onEscapeKeyPressed() {
        if (!endProjectionButton.isDisabled()) {
            endProjectionButton.fire();
        }
    }
}
