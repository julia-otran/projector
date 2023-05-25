/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import dev.juhouse.projector.projection2.image.ProjectionImage;
import dev.juhouse.projector.projection2.ProjectionManager;
import javafx.application.Platform;
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
import lombok.Getter;

/**
 * FXML Controller class
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ImageController extends ProjectionController implements Runnable, ProjectionBarControlCallbacks {
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());
    }

    @FXML
    private Pane projectionControlPane;

    private final ProjectionBarControl controlBar = new ProjectionBarControl();

    @FXML
    private Label dragDropLabel;

    @FXML
    private Pane imagePane;

    @FXML
    private ImageView imageView;

    private boolean running = false;

    private final List<File> openedImages = new ArrayList<>();

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

    private final DecimalFormat milisecondsFormatter = new DecimalFormat("#0.000");

    private static class ImageListCell extends ListCell<Image> {
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

        Callback<ListView<Image>, ListCell<Image>> cellFactory = listView -> new ImageListCell();

        imagesList.setCellFactory(cellFactory);

        imagesList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            Image image = imagesList.getItems().get(newValue.intValue());
            projectable.setModel(() -> image);
            imageView.setImage(image);
        });

        formatTimeLabel();

        changeMsecSlider.valueProperty().addListener((observable, oldValue, newValue) -> formatTimeLabel());

        controlBar.setCanProject(false);
        controlBar.setProjectable(this.projectable);
        controlBar.setCallback(this);
        controlBar.setManager(projectionManager);
        controlBar.attach(projectionControlPane);

        loadOpenedImages();
    }

    private record ToAddImage(@Getter Image image, @Getter File file) { }

    private final List<ToAddImage> toAdd = new ArrayList<>();

    @FXML
    public void onDragOver(DragEvent event) {
        toAdd.clear();
        Dragboard board = event.getDragboard();

        if (board.hasFiles()) {
            for (File imageFile : board.getFiles()) {
                try {
                    Image img = new Image(imageFile.toURI().toURL().toExternalForm());
                    toAdd.add(new ToAddImage(img, imageFile));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (toAdd.size() > 0) {
                imageView.setImage(toAdd.get(0).image());
                dragDropLabel.setText(toAdd.size() + " arquivos a serem adicionados");
                event.acceptTransferModes(TransferMode.LINK);
            }
        } else if (board.hasImage()) {
            toAdd.add(new ToAddImage(board.getImage(), null));
            dragDropLabel.setText("1 imagem a ser adicionada");
            event.acceptTransferModes(TransferMode.LINK);
        } else {
            setError();
        }


    }

    @FXML
    public void onDragExit() {
        toAdd.clear();
        setOriginal();
    }

    @FXML
    public void onDragDropped(DragEvent event) {
        for (ToAddImage adding : toAdd) {
            if (adding.image().isError()) {
                System.out.println("Image errored");
                System.out.println(adding.image().exceptionProperty().get().toString());
                continue;
            }

            imagesList.getItems().add(adding.image());

            if (adding.file() != null) {
                openedImages.add(adding.file());
            }
        }

        if (imagesList.getItems().size() > 0) {
            controlBar.setCanProject(true);
        }

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
        String countStr = getObserver().getProperty("IMAGES_COUNT").orElse(null);

        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            for (int i=0; i<count; i++) {
                String imgPath = getObserver().getProperty("IMAGE[" + i + "]").orElse(null);
                if (imgPath != null) {
                    File file = new File(imgPath);

                    if (file.canRead()) {
                        try {
                            Image img = new Image(file.toURI().toString());
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
            controlBar.setCanProject(true);
        }

        setOriginal();
    }

    private void setError() {
        dragDropLabel.setVisible(true);
        dragDropLabel.setText("Mídia inaceitável");
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

    @Override
    public void onProjectionBegin() {
        getProjectionManager().setProjectable(projectable);
        start();
    }

    @Override
    public void onProjectionEnd() {
        getProjectionManager().setProjectable(null);
        stopRunning();
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        projectionManager.stop(projectable);
    }

    private void start() {
        if (running) {
            return;
        }

        running = true;
        Thread timerThread = new Thread(this);
        timerThread.start();
    }

    private void stopRunning() {
        running = false;
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        long time = System.currentTimeMillis();

        while (running) {
            long current = System.currentTimeMillis();
            double interval = changeMsecSlider.valueProperty().doubleValue() * 1000;

            if (current - time < Math.round(interval) || interval > 180.0 * 1000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            time = current;

            int newIndex = getProjectingIndex() + 1;
            final int nextIndex;

            if (newIndex >= imagesList.getItems().size()) {
                nextIndex = 0;
            } else {
                nextIndex = newIndex;
            }

            Platform.runLater(() -> imagesList.getSelectionModel().clearAndSelect(nextIndex));
        }
    }

    @Override
    public void onEscapeKeyPressed() {
        if (controlBar.getProjecting()) {
            onProjectionEnd();
        }
    }

    private void formatTimeLabel() {
        double secs = changeMsecSlider.getValue();

        if (secs > 180.0) {
            timeLabel.setText("Trocar imagens manualmente");
        } else {
            String time = milisecondsFormatter.format(secs);
            timeLabel.setText("Trocar imagem a cada " + time + " segundos");
        }
    }

    @FXML
    public void onCropImageChanged() {
        projectable.setCropBackground(cropImageCheckBox.isSelected());
    }
}
