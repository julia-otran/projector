/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers.projection;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import us.guihouse.projector.forms.controllers.projection.ProjectionController;
import us.guihouse.projector.projection.ProjectionImage;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.models.BackgroundProvide;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class ImageController extends ProjectionController implements Runnable {
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(true);
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());
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

    private boolean running = false;
    private List<BufferedImage> awtImages = new ArrayList<>();
    private List<File> openedImages = new ArrayList<>();

    @FXML
    private ListView<Image> imagesList;

    @FXML
    private Label timeLabel;

    @FXML
    private Slider changeMsecSlider;

    @FXML
    private CheckBox cropImageCheckBox;

    private ProjectionImage projectable;

    private String oldLabelText;

    private DecimalFormat milisecondsFormatter = new DecimalFormat("#0.000");

    private class ImageListCell extends ListCell<Image> {
        @Override
        protected void updateItem(Image item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(null);
                ImageView img = new ImageView(item);
                img.setFitWidth(70.0);
                img.setFitHeight(70.0);
                img.preserveRatioProperty().set(true);
                setGraphic(img);
            }
        }
    }

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        this.projectable = projectionManager.createImage();
        projectable.setCropBackground(false);
        oldLabelText = dragDropLabel.getText();

        Callback<ListView<Image>, ListCell<Image>> cellFactory = new Callback<ListView<Image>, ListCell<Image>>() {
            @Override public ListCell<Image> call(ListView<Image> listView) {
                return new ImageListCell();
            }
        };

        imagesList.setCellFactory(cellFactory);

        imagesList.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                projectable.setModel(new BackgroundProvide() {
                    @Override
                    public BufferedImage getBackground() {
                        return null;
                    }

                    @Override
                    public BufferedImage getLogo() {
                        return null;
                    }

                    @Override
                    public BufferedImage getOverlay() {
                        return null;
                    }

                    @Override
                    public BufferedImage getStaticBackground() {
                        return awtImages.get(newValue.intValue());
                    }

                    @Override
                    public Type getType() {
                        return Type.STATIC;
                    }
                });

                imageView.setImage(imagesList.getItems().get(newValue.intValue()));
            }
        });

        formatTimeLabel();

        changeMsecSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                formatTimeLabel();
            }
        });

        loadOpenedImages();
    }

    private List<Image> toAdd = new ArrayList<>();
    private List<File> toAddFiles = new ArrayList<>();

    @FXML
    public void onDragOver(DragEvent event) {
        toAdd.clear();
        toAddFiles.clear();
        Dragboard board = event.getDragboard();

        if (board.hasFiles()) {
            for (File imageFile : board.getFiles()) {
                try {
                    Image img = new Image(imageFile.toURI().toString());
                    toAdd.add(img);
                    toAddFiles.add(imageFile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (toAdd.size() > 0) {
                imageView.setImage(toAdd.get(0));
                dragDropLabel.setText(toAdd.size() + " arquivos a serem adicionados");
                event.acceptTransferModes(TransferMode.LINK);
            }
        } else {
            setError("Mídia inaceitável");
        }
    }

    @FXML
    public void onDragExit() {
        toAdd.clear();
        setOriginal();
    }

    @FXML
    public void onDragDropped(DragEvent event) {
        for (Image adding : toAdd) {
            if (adding.isError()) {
                continue;
            }

            BufferedImage awt = SwingFXUtils.fromFXImage(adding, null);
            awtImages.add(awt);
            imagesList.getItems().add(adding);
        }

        if (imagesList.getItems().size() > 0) {
            beginProjectionButton.setDisable(false);
        }

        openedImages.addAll(toAddFiles);

        saveOpenedImages();

        setOriginal();
        toAdd.clear();
    }

    private void saveOpenedImages() {
        getObserver().updateProperty("IMAGES_COUNT", Integer.toString(openedImages.size()));

        int fileIndex = 0;

        for (File file : openedImages) {
            getObserver().updateProperty("IMAGE[" + fileIndex + "]", file.toString());
            fileIndex++;
        }
    }

    private void loadOpenedImages() {
        String countStr = getObserver().getProperty("IMAGES_COUNT");

        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            for (int i=0; i<count; i++) {
                String imgPath = getObserver().getProperty("IMAGE[" + i + "]");
                if (imgPath != null) {
                    File file = new File(imgPath);

                    if (file.canRead()) {
                        try {
                            Image img = new Image(file.toURI().toString());
                            BufferedImage awt = SwingFXUtils.fromFXImage(img, null);
                            awtImages.add(awt);
                            imagesList.getItems().add(img);
                            openedImages.add(file);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        if (imagesList.getItems().size() > 0) {
            beginProjectionButton.setDisable(false);
        }

        setOriginal();
    }

    private void setError(String error) {
        dragDropLabel.setVisible(true);
        dragDropLabel.setText(error);
        imagePane.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }

    private int getProjectingIndex() {
        return imagesList.getSelectionModel().getSelectedIndex();
    }

    private void setOriginal() {
        dragDropLabel.setText(oldLabelText);
        imagePane.setBorder(null);

        if (getProjectingIndex() >= 0 && getProjectingIndex() < imagesList.getItems().size()) {
            imageView.setImage(imagesList.getItems().get(getProjectingIndex()));
        } else if (imagesList.getItems().size() > 0) {
            imagesList.getSelectionModel().select(0);
        } else {
            imageView.setImage(null);
        }
    }

    @FXML
    public void onBeginProjection() {
        getProjectionManager().setProjectable(projectable);

        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(false);
        start();
    }

    @FXML
    public void onEndProjection() {
        getProjectionManager().setProjectable(null);
        beginProjectionButton.disableProperty().set(false);
        endProjectionButton.disableProperty().set(true);
        stopRunning();
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        projectionManager.stop(projectable);
    }

    private void start() {
        running = true;
        Platform.runLater(this);
    }

    private void stopRunning() {
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
        double interval = changeMsecSlider.valueProperty().doubleValue() * 1000;

        if (current - time < Math.round(interval)) {
            return;
        }

        if (time == 0) {
            time = current;
            return;
        }

        time = current;

        int nextIndex = getProjectingIndex() + 1;
        if (nextIndex >= imagesList.getItems().size()) {
            nextIndex = 0;
        }

        imagesList.getSelectionModel().clearAndSelect(nextIndex);
    }

    @Override
    public void onEscapeKeyPressed() {
        if (!endProjectionButton.isDisabled()) {
            endProjectionButton.fire();
        }
    }

    private void formatTimeLabel() {
        double secs = changeMsecSlider.getValue();
        String time = milisecondsFormatter.format(secs);
        timeLabel.setText(time);
    }

    @FXML
    public void onCropImageChanged() {
        projectable.setCropBackground(cropImageCheckBox.isSelected());
    }
}
