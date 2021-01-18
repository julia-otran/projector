package us.guihouse.projector.projection.video;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import us.guihouse.projector.projection.CanvasDelegate;
import us.guihouse.projector.projection.Projectable;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.utils.ThemeFinder;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ProjectionBackgroundVideo implements Projectable, ProjectionBackgroundVideoLoop.LoopCallback {
    private final List<File> media = new ArrayList<>();
    private File currentMedia = null;
    private final Map<Integer, File> musicMap = new HashMap<>();
    private final Random random = new Random();
    private final ProjectionVideo[] videoProjectors = new ProjectionVideo[2];
    private boolean playing;
    private int currentPlayer = 0;

    private final ReadOnlyBooleanWrapper render = new ReadOnlyBooleanWrapper();

    public ProjectionBackgroundVideo(CanvasDelegate delegate) {
        videoProjectors[0] = new ProjectionVideo(delegate);
        videoProjectors[1] = new ProjectionVideo(delegate);

        videoProjectors[0].setCropVideo(true);
        videoProjectors[1].setCropVideo(true);

        playing = false;

        videoProjectors[0].getRender().addListener((prop, oldVal, newVal) -> updateRender());
        videoProjectors[1].getRender().addListener((prop, oldVal, newVal) -> updateRender());
    }

    private void updateRender() {
        render.set(videoProjectors[0].getRender().get() || videoProjectors[1].getRender().get());
    }

    public ReadOnlyBooleanProperty isRender() {
        return render.getReadOnlyProperty();
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        videoProjectors[0].paintComponent(g, vs);
        videoProjectors[1].paintComponent(g, vs);
    }

    @Override
    public void rebuildLayout() {
        videoProjectors[0].rebuildLayout();
        videoProjectors[1].rebuildLayout();
    }

    @Override
    public void init() {
        videoProjectors[0].init();
        videoProjectors[0].getPlayer().audio().setMute(true);
        videoProjectors[0].getPlayer().events().addMediaPlayerEventListener(new ProjectionBackgroundVideoLoop(0, this));
        videoProjectors[0].getRender().setValue(false);

        videoProjectors[1].init();
        videoProjectors[1].getPlayer().audio().setMute(true);
        videoProjectors[1].getPlayer().events().addMediaPlayerEventListener(new ProjectionBackgroundVideoLoop(1, this));
        videoProjectors[1].getRender().setValue(false);

        loadMedia();
    }

    @Override
    public void finish() {
        videoProjectors[0].finish();
        videoProjectors[1].finish();
    }

    public void loadMedia() {
        ThemeFinder.getThemes().forEach(t -> media.add(t.getVideoFile()));
    }

    public void startBackground(Integer musicId, File preferred) {
        if (preferred != null) {
            musicMap.put(musicId, preferred);
        }

        if (!musicMap.containsKey(musicId)) {
            if (media.isEmpty()) {
                return;
            }

            int id = Math.abs(random.nextInt()) % media.size();
            musicMap.put(musicId, media.get(id));
        }

        File toPlay = musicMap.get(musicId);

        if (toPlay != null) {
            if (toPlay.equals(currentMedia)) {
                if (!playing) {
                    playMedia(toPlay);
                }
            } else {
                playMedia(toPlay);
            }
        }
    }

    private void playMedia(File toPlay) {
        stopBackground();
        currentMedia = toPlay;
        playing = true;
        currentPlayer = 0;
        videoProjectors[0].getPlayer().media().play(toPlay.getAbsolutePath());
        videoProjectors[1].getPlayer().media().prepare(toPlay.getAbsolutePath());
        videoProjectors[0].getRender().setValue(true);
    }

    public void stopBackground() {
        videoProjectors[0].getRender().setValue(false);
        videoProjectors[1].getRender().setValue(false);
        videoProjectors[0].getPlayer().controls().stop();
        videoProjectors[1].getPlayer().controls().stop();

        playing = false;
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, int playerIndex, float position) {
        if (playerIndex == currentPlayer) {
            if (Float.compare(position, 0.99f) > 0) {
                swapPlayersIfNeeded();
            }
        } else {
            mediaPlayer.controls().pause();
        }
    }

    @Override
    public void playing(MediaPlayer mediaPlayer, int playerIndex) {
        if (playerIndex == currentPlayer) {
            if (playerIndex == 0) {
                if (!videoProjectors[0].getRender().get()) {
                    videoProjectors[0].getRender().set(true);
                    videoProjectors[1].getRender().set(false);
                    videoProjectors[1].getPlayer().controls().setPosition(0);
                    videoProjectors[1].getPlayer().controls().play();
                }
            } else {
                if (!videoProjectors[1].getRender().get()) {
                    videoProjectors[1].getRender().set(true);
                    videoProjectors[0].getRender().set(false);
                    videoProjectors[0].getPlayer().controls().setPosition(0);
                    videoProjectors[0].getPlayer().controls().play();
                }
            }
        }
    }

    @Override
    public void finished(MediaPlayer mediaPlayer, int playerIndex) {
        if (playerIndex == currentPlayer) {
            swapPlayersIfNeeded();
        }
    }

    private void swapPlayersIfNeeded() {
        if (currentPlayer == 0) {
            if (!videoProjectors[1].getPlayer().status().isPlaying()) {
                currentPlayer = 1;
                videoProjectors[1].getPlayer().controls().play();
            }
        } else {
            if (!videoProjectors[0].getPlayer().status().isPlaying()) {
                currentPlayer = 0;
                videoProjectors[0].getPlayer().controls().play();
            }
        }
    }
}
