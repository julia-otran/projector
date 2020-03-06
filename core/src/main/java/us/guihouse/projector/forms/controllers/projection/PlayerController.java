/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers.projection;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import us.guihouse.projector.other.ResizeableSwingNode;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.video.ProjectionPlayer;
import us.guihouse.projector.services.FileDragDropService;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class PlayerController extends ProjectionController implements FileDragDropService.Client, MediaPlayerEventListener {

    private FileDragDropService dragDropService;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        endProjectionButton.disableProperty().set(true);
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
    private Button beginProjectionButton;

    @FXML
    private Button endProjectionButton;

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
    public void onBeginProjection() {
        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(false);
        getProjectionManager().setProjectable(projectionPlayer);

        withSoundButton.fire();
        withSoundButton.setSelected(true);
    }

    @FXML
    public void onEndProjection() {
        beginProjectionButton.disableProperty().set(false);
        endProjectionButton.disableProperty().set(true);
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
        projectionPlayer.getPreviewPanel().fitWidthProperty().bind(playerBox.widthProperty());
        projectionPlayer.getPreviewPanel().fitHeightProperty().bind(playerContainer.heightProperty());

        playerBox.setVisible(false);
        chooseFileBox.setVisible(true);

        withoutSoundButton.fire();
        withoutSoundButton.setSelected(true);

        projectionPlayer.getPlayer().addMediaPlayerEventListener(this);

        timeBar.valueChangingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                manualMoving = newValue;
            }
        });

        timeBar.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (!automaticMoving) {
                    projectionPlayer.getPlayer().setPosition(timeBar.valueProperty().floatValue());
                }
            }
        });

        String fileStr = getObserver().getProperty("VIDEO_FILE");
        if (fileStr != null) {
            File file = new File(fileStr);
            if (file.canRead()) {
                openMedia(file);
            }
        }
    }

    @Override
    public void onEscapeKeyPressed() {
        if (!endProjectionButton.isDisabled()) {
            endProjectionButton.fire();
        }
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        projectionPlayer.getPlayer().stop();
        projectionManager.stop(projectionPlayer);

    }

    @FXML
    public void playButtonClick() {
        projectionPlayer.getPlayer().play();
    }

    @FXML
    public void pauseButtonClick() {
        projectionPlayer.getPlayer().pause();
    }

    @FXML
    public void stopButtonClick() {
        projectionPlayer.getPlayer().stop();
        projectionPlayer.getPlayer().setPosition(0);
    }

    @FXML
    public void repeatButtonClick() {
        projectionPlayer.getPlayer().setRepeat(repeatButton.isSelected());
    }

    @FXML
    public void withoutSoundButtonClick() {
        projectionPlayer.getPlayer().mute(true);
    }

    @FXML
    public void withSoundButtonClick() {
        projectionPlayer.getPlayer().mute(false);
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
    public void selectFileClick() {
        FileChooser chooser = new FileChooser();
        Stage stage = getSceneManager().getStage();
        File chosen = chooser.showOpenDialog(stage);

        if (chosen != null && chosen.canRead()) {
            openMedia(chosen);
        }
    }

    private void openMedia(File file) {
        projectionPlayer.loadMedia(file);
        chooseFileBox.setVisible(false);
        playerBox.setVisible(true);

        playButton.disableProperty().set(false);
        pauseButton.disableProperty().set(true);
        stopButton.disableProperty().set(true);
    }

    @Override
    public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t libvlc_media_t, String s) {

    }

    @Override
    public void opening(MediaPlayer mediaPlayer) {

    }

    @Override
    public void buffering(MediaPlayer mediaPlayer, float v) {

    }

    @Override
    public void playing(MediaPlayer mediaPlayer) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                playButton.disableProperty().set(true);
                pauseButton.disableProperty().set(false);
                stopButton.disableProperty().set(false);
            }
        });
    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                playButton.disableProperty().set(false);
                pauseButton.disableProperty().set(true);
                stopButton.disableProperty().set(false);
            }
        });

    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                playButton.disableProperty().set(false);
                pauseButton.disableProperty().set(true);
                stopButton.disableProperty().set(true);
            }
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

        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                long secs = currentTime % 60;
                long mins = (currentTime / 60) % 60;
                long hours = currentTime / 3600;

                timeLabel.setText(String.format("%02d:%02d:%02d", hours, mins, secs));
            }
        });
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float v) {
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
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
            }
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
    public void elementaryStreamAdded(MediaPlayer mediaPlayer, int i, int i1) {

    }

    @Override
    public void elementaryStreamDeleted(MediaPlayer mediaPlayer, int i, int i1) {

    }

    @Override
    public void elementaryStreamSelected(MediaPlayer mediaPlayer, int i, int i1) {

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

    @Override
    public void mediaMetaChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t libvlc_media_t) {

    }

    @Override
    public void mediaDurationChanged(MediaPlayer mediaPlayer, long l) {

    }

    @Override
    public void mediaParsedChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void mediaParsedStatus(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void mediaFreed(MediaPlayer mediaPlayer) {

    }

    @Override
    public void mediaStateChanged(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void mediaSubItemTreeAdded(MediaPlayer mediaPlayer, libvlc_media_t libvlc_media_t) {

    }

    @Override
    public void newMedia(MediaPlayer mediaPlayer) {
        Platform.runLater(() -> {
            notifyTitleChange(mediaPlayer.getMediaMetaData().getTitle());
        });
    }

    @Override
    public void subItemPlayed(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void subItemFinished(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void endOfSubItems(MediaPlayer mediaPlayer) {

    }

}
