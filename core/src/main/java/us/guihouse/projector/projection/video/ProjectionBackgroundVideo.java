package us.guihouse.projector.projection.video;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import uk.co.caprica.vlcj.media.MediaEventListener;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import us.guihouse.projector.projection.CanvasDelegate;
import us.guihouse.projector.projection.Projectable;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.utils.ThemeFinder;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ProjectionBackgroundVideo implements Projectable, ProjectionBackgroundVideoCallbacks.Callbacks {
    private final List<File> media = new ArrayList<>();
    private File currentMedia = null;
    private final Map<Integer, File> musicMap = new HashMap<>();
    private final Random random = new Random();
    private final ProjectionVideo videoProjector;
    private boolean playing;

    public ProjectionBackgroundVideo(CanvasDelegate delegate) {
        videoProjector = new ProjectionVideo(delegate);
        videoProjector.setCropVideo(true);
    }

    public ReadOnlyBooleanProperty isRender() {
        return videoProjector.getRender();
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        videoProjector.paintComponent(g, vs);
    }

    @Override
    public void rebuildLayout() {
        videoProjector.rebuildLayout();
    }

    @Override
    public void init() {
        videoProjector.init();
        videoProjector.getPlayer().audio().setMute(true);
        videoProjector.getRender().setValue(false);
        videoProjector.getPlayer().events().addMediaPlayerEventListener(new ProjectionBackgroundVideoCallbacks(this));
        loadMedia();
    }

    @Override
    public void finish() {
        videoProjector.finish();
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
        videoProjector.getPlayer().media().play(toPlay.getAbsolutePath());
        videoProjector.getPlayer().controls().setRepeat(true);
        playing = true;
    }

    public void stopBackground() {
        playing = false;
        videoProjector.getRender().setValue(false);
        videoProjector.getPlayer().controls().stop();
    }

    @Override
    public void mediaPlayerReady(MediaPlayer mediaPlayer) {
        videoProjector.getRender().setValue(true);
    }
}
