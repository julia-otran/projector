package us.guihouse.projector.projection;

import javafx.beans.binding.ObjectExpression;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import us.guihouse.projector.utils.ThemeFinder;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import static us.guihouse.projector.utils.FilePaths.PROJECTOR_BACKGROUND_VIDEOS;

public class ProjectionBackgroundVideo implements Projectable, ProjectionBackgroundVideoLoop.LoopCallback {
    private CanvasDelegate canvasDelegate;
    private List<File> media = new ArrayList<>();
    private File currentMedia = null;
    private Map<Integer, File> musicMap = new HashMap<>();
    private Random random = new Random();
    private ProjectionVideo videoProjectors[] = new ProjectionVideo[2];
    private boolean playing;
    private int currentPlayer = 0;

    public ProjectionBackgroundVideo(CanvasDelegate delegate) {
        this.canvasDelegate = delegate;
        videoProjectors[0] = new ProjectionVideo(delegate, true);
        videoProjectors[1] = new ProjectionVideo(delegate, true);
        playing = false;
    }

    public boolean isRender() {
        return videoProjectors[0].isRender() || videoProjectors[1].isRender();
    }

    @Override
    public void paintComponent(Graphics2D g) {
        videoProjectors[0].paintComponent(g);
        videoProjectors[1].paintComponent(g);
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return this.canvasDelegate;
    }

    @Override
    public void rebuildLayout() {
        videoProjectors[0].rebuildLayout();
        videoProjectors[1].rebuildLayout();
        init();
    }

    @Override
    public void init() {
        videoProjectors[0].init();
        videoProjectors[0].getPlayer().mute(true);
        videoProjectors[0].getPlayer().addMediaPlayerEventListener(new ProjectionBackgroundVideoLoop(0, this));
        videoProjectors[0].setRender(false);

        videoProjectors[1].init();
        videoProjectors[1].getPlayer().mute(true);
        videoProjectors[1].getPlayer().addMediaPlayerEventListener(new ProjectionBackgroundVideoLoop(1, this));
        videoProjectors[1].setRender(false);

        loadMedia();

        if (playing) {
            playMedia(currentMedia);
        }
    }

    @Override
    public void finish() {
        videoProjectors[0].finish();
        videoProjectors[1].finish();
    }

    public void loadMedia() {
        ThemeFinder.getThemes().forEach(t -> {
            media.add(t.getVideoFile());
            videoProjectors[0].getPlayer().prepareMedia(t.getVideoFile().getAbsolutePath());
            videoProjectors[1].getPlayer().prepareMedia(t.getVideoFile().getAbsolutePath());
        });
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
        videoProjectors[0].setRender(true);
        videoProjectors[0].getPlayer().playMedia(toPlay.getAbsolutePath());
        videoProjectors[1].getPlayer().playMedia(toPlay.getAbsolutePath());
    }

    public void stopBackground() {
        videoProjectors[0].setRender(false);
        videoProjectors[1].setRender(false);
        videoProjectors[0].getPlayer().stop();
        videoProjectors[1].getPlayer().stop();
        playing = false;
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, int playerIndex, float position) {
        if (playerIndex == currentPlayer) {
            if (Float.compare(position, 0.99f) > 0) {
                swapPlayersIfNeeded();
            }
        } else {
            mediaPlayer.pause();
        }
    }

    @Override
    public void playing(MediaPlayer mediaPlayer, int playerIndex) {
        if (playerIndex == currentPlayer) {
            if (playerIndex == 0) {
                if (!videoProjectors[0].isRender()) {
                    videoProjectors[0].setRender(true);
                    videoProjectors[1].setRender(false);
                    videoProjectors[1].getPlayer().setPosition(0);
                    videoProjectors[1].getPlayer().play();
                }
            } else {
                if (!videoProjectors[1].isRender()) {
                    videoProjectors[1].setRender(true);
                    videoProjectors[0].setRender(false);
                    videoProjectors[0].getPlayer().setPosition(0);
                    videoProjectors[0].getPlayer().play();
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
            if (!videoProjectors[1].getPlayer().isPlaying()) {
                currentPlayer = 1;
                videoProjectors[1].getPlayer().play();
            }
        } else {
            if (!videoProjectors[0].getPlayer().isPlaying()) {
                currentPlayer = 0;
                videoProjectors[0].getPlayer().play();
            }
        }
    }
}
