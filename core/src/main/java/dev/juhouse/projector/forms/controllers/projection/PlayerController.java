/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import dev.juhouse.projector.projection2.ProjectionManager;
import dev.juhouse.projector.projection2.video.ProjectionPlayer;
import dev.juhouse.projector.services.FileDragDropService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;

/**
 * FXML Controller class
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class PlayerController extends ProjectionController implements FileDragDropService.Client, MediaPlayerEventListener, ProjectionBarControlCallbacks {

    private FileDragDropService dragDropService;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    private ProjectionPlayer projectionPlayer;

    // Drag and drop
    @FXML
    private Label dragDropLabel;

    @FXML
    private VBox chooseFileBox;

    @FXML
    private VBox playerBox;

    private String oldLabelText;

    // Controls
    @FXML
    private Pane projectionControlPane;

    private final ProjectionBarControl controlBar = new ProjectionBarControl();

    @FXML
    private BorderPane playerContainer;

    // Player controls
    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button stopButton;

    @FXML
    private ToggleButton repeatButton;

    @FXML
    private Slider timeBar;
    private boolean automaticMoving;
    private boolean manualMoving;

    @FXML
    private Label timeLabel;

    @FXML
    private ToggleButton withoutSoundButton;

    @FXML
    private ToggleButton withSoundButton;

    @FXML
    private CheckBox fullScreenCheckBox;

    @Override
    public void onProjectionBegin() {
        getProjectionManager().setProjectable(projectionPlayer);

        withSoundButton.fire();
        withSoundButton.setSelected(true);
    }

    @Override
    public void onProjectionEnd() {
        getProjectionManager().setProjectable(null);

        withoutSoundButton.fire();
        withoutSoundButton.setSelected(true);
    }

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        this.oldLabelText = dragDropLabel.getText();

        this.dragDropService = new FileDragDropService(this);

        try {
            this.projectionPlayer = projectionManager.createPlayer();
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Show Error State
            return;
        }

        playerContainer.setCenter(projectionPlayer.getPreviewPanel());
        projectionPlayer.getPreviewPanel().prefWidthProperty().bind(playerBox.widthProperty());
        projectionPlayer.getPreviewPanel().prefHeightProperty().bind(playerContainer.heightProperty());

        playerBox.setVisible(false);
        chooseFileBox.setVisible(true);

        withoutSoundButton.fire();
        withoutSoundButton.setSelected(true);

        projectionPlayer.getPlayer().events().addMediaPlayerEventListener(this);

        timeBar.valueChangingProperty().addListener((observable, oldValue, newValue) -> manualMoving = newValue);

        timeBar.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!automaticMoving) {
                projectionPlayer.getPlayer().controls().setPosition(timeBar.valueProperty().floatValue());
            }
        });

        String fileStr = getObserver().getProperty("VIDEO_FILE").orElse(null);

        if (fileStr != null) {
            File file = new File(fileStr);
            if (file.canRead()) {
                openMedia(file);
            }
        }

        fullScreenCheckBox.setSelected(projectionPlayer.isCropVideo());

        controlBar.setProjectable(projectionPlayer);
        controlBar.setCallback(this);
        controlBar.setManager(projectionManager);
        controlBar.attach(projectionControlPane);
    }

    @Override
    public void onEscapeKeyPressed() {
        if (controlBar.getProjecting()) {
            onProjectionEnd();
        }
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        projectionPlayer.getPlayer().controls().stop();
        projectionManager.stop(projectionPlayer);
        projectionPlayer.getPreviewPanel().stopPreview();
    }

    @FXML
    public void playButtonClick() {
        projectionPlayer.getPlayer().controls().play();
    }

    @FXML
    public void pauseButtonClick() {
        projectionPlayer.getPlayer().controls().pause();
    }

    @FXML
    public void stopButtonClick() {
        projectionPlayer.getPlayer().controls().stop();
        projectionPlayer.getPlayer().controls().setPosition(0);
    }

    @FXML
    public void repeatButtonClick() {
        projectionPlayer.getPlayer().controls().setRepeat(repeatButton.isSelected());
    }

    @FXML
    public void withoutSoundButtonClick() {
        projectionPlayer.getPlayer().audio().setMute(true);
    }

    @FXML
    public void withSoundButtonClick() {
        projectionPlayer.getPlayer().audio().setMute(false);
    }

    // Drag and drop
    @FXML
    public void onDragOver(DragEvent event) {
        dragDropService.onDragOver(event);
    }

    @FXML
    public void onDragExit() {
        dragDropService.onDragExit();
    }

    @FXML
    public void onDragDropped(DragEvent event) {
        dragDropService.onDragDropped(event);
    }

    private void setOriginal() {
        dragDropLabel.setText(oldLabelText);
        chooseFileBox.setBorder(null);
    }

    @Override
    public void onFileOk() {
        dragDropLabel.setText("Solte na área demarcada");
        chooseFileBox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }

    @Override
    public void onFileError(String message) {
        dragDropLabel.setVisible(true);
        dragDropLabel.setText(message);
        chooseFileBox.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }

    @Override
    public void onDropSuccess(File file) {
        getObserver().updateProperty("VIDEO_FILE", file.toString());
        openMedia(file);
    }

    @Override
    public void onDropAbort() {
        setOriginal();
        dragDropLabel.setVisible(false);
    }

    @FXML
    public void onFullScreenAction() {
        projectionPlayer.setCropVideo(fullScreenCheckBox.isSelected());
    }

    @Override
    public void setVisible(Boolean visible) {
        super.setVisible(visible);

        if (visible) {
            projectionPlayer.getPreviewPanel().startPreview();
        } else {
            projectionPlayer.getPreviewPanel().stopPreview();
        }
    }

    private void openMedia(File file) {
        projectionPlayer.loadMedia(file);
        notifyTitleChange(file.getName());

        chooseFileBox.setVisible(false);
        playerBox.setVisible(true);

        playButton.disableProperty().set(false);
        pauseButton.disableProperty().set(true);
        stopButton.disableProperty().set(true);
    }

    @Override
    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {

    }

    @Override
    public void opening(MediaPlayer mediaPlayer) {

    }

    @Override
    public void buffering(MediaPlayer mediaPlayer, float v) {

    }

    @Override
    public void playing(MediaPlayer mediaPlayer) {
        Platform.runLater(() -> {
            playButton.disableProperty().set(true);
            pauseButton.disableProperty().set(false);
            stopButton.disableProperty().set(false);
        });
    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
        Platform.runLater(() -> {
            playButton.disableProperty().set(false);
            pauseButton.disableProperty().set(true);
            stopButton.disableProperty().set(false);
        });

    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {
        Platform.runLater(() -> {
            playButton.disableProperty().set(false);
            pauseButton.disableProperty().set(true);
            stopButton.disableProperty().set(true);
        });
    }

    @Override
    public void forward(MediaPlayer mediaPlayer) {

    }

    @Override
    public void backward(MediaPlayer mediaPlayer) {

    }

    @Override
    public void finished(MediaPlayer mediaPlayer) {

    }

    private long currentTime;

    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long l) {
        if (currentTime == l / 1000) {
            return;
        }

        currentTime = l / 1000;

        Platform.runLater(() -> {
            long secs = currentTime % 60;
            long mins = (currentTime / 60) % 60;
            long hours = currentTime / 3600;

            timeLabel.setText(String.format("%02d:%02d:%02d", hours, mins, secs));
        });
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float v) {
        Platform.runLater(() -> {
            if (manualMoving) {
                return;
            }

            automaticMoving = true;

            if (Float.compare(v, 0) < 0) {
                timeBar.valueProperty().set(0);
            } else if (Float.compare(v, 1) > 0) {
                timeBar.valueProperty().set(1);
            } else {
                timeBar.valueProperty().set(v);
            }

            automaticMoving = false;
        });
    }

    @Override
    public void seekableChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void pausableChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void titleChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void snapshotTaken(MediaPlayer mediaPlayer, String s) {

    }

    @Override
    public void lengthChanged(MediaPlayer mediaPlayer, long l) {

    }

    @Override
    public void videoOutput(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void scrambledChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType type, int id) {

    }

    @Override
    public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType type, int id) {

    }

    @Override
    public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType type, int id) {

    }

    @Override
    public void corked(MediaPlayer mediaPlayer, boolean b) {

    }

    @Override
    public void muted(MediaPlayer mediaPlayer, boolean b) {

    }

    @Override
    public void volumeChanged(MediaPlayer mediaPlayer, float v) {

    }

    @Override
    public void audioDeviceChanged(MediaPlayer mediaPlayer, String s) {

    }

    @Override
    public void chapterChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void error(MediaPlayer mediaPlayer) {

    }

    @Override
    public void mediaPlayerReady(MediaPlayer mediaPlayer) {

    }
}
