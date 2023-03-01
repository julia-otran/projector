package dev.juhouse.projector.projection2.video;

import dev.juhouse.projector.projection2.Projectable;
import dev.juhouse.projector.utils.ThemeFinder;

import java.io.File;
import java.util.*;
import java.util.List;

public class ProjectionBackgroundVideo implements Projectable {
    private final List<File> media = new ArrayList<>();
    private File currentMedia = null;
    private final Map<Integer, File> musicMap = new HashMap<>();
    private final Random random = new Random();
    private final ProjectionVideo videoProjector;
    private boolean playing;

    private boolean externalRender;
    private boolean internalRender;

    public ProjectionBackgroundVideo(ProjectionVideo videoProjector) {
        this.videoProjector = videoProjector;
        videoProjector.setCropVideo(true);
    }

    @Override
    public void init() {
        videoProjector.init();
        videoProjector.getPlayer().audio().setMute(true);
        loadMedia();
    }

    @Override
    public void finish() {
        videoProjector.finish();
    }

    @Override
    public void rebuild() {

    }

    @Override
    public void setRender(boolean render) {
        externalRender = render;
        updateRender();
    }

    private void updateRender() {
        videoProjector.setRender(externalRender && internalRender);
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
        if (playing) {
            stopBackground();
        }

        currentMedia = toPlay;
        videoProjector.getPlayer().media().play(toPlay.getAbsolutePath());
        videoProjector.getPlayer().controls().setRepeat(true);

        internalRender = true;
        updateRender();

        playing = true;
    }

    public void stopBackground() {
        playing = false;
        videoProjector.getPlayer().controls().pause();

        internalRender = false;
        updateRender();
    }
}
