package dev.juhouse.projector.projection2.video;

import dev.juhouse.projector.projection2.BridgeRender;
import dev.juhouse.projector.projection2.BridgeRenderFlag;
import dev.juhouse.projector.projection2.Projectable;
import dev.juhouse.projector.utils.ThemeFinder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;

import java.io.File;
import java.util.*;
import java.util.List;

public class ProjectionBackgroundVideo implements Projectable, MediaPlayerEventListener {
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
    }

    @Override
    public void init() {
        videoProjector.init();
        videoProjector.setCropVideo(true);
        videoProjector.getPlayer().audio().setMute(true);
        videoProjector.getRenderFlagProperty().get().applyDefault(BridgeRender::getEnableRenderBackgroundAssets);
        videoProjector.getPlayer().events().addMediaPlayerEventListener(this);
        loadMedia();
    }

    @Override
    public void finish() {
        videoProjector.finish();
    }

    @Override
    public void rebuild() {
        videoProjector.getRenderFlagProperty().get().applyDefault(BridgeRender::getEnableRenderBackgroundAssets);
        videoProjector.rebuild();
    }

    @Override
    public void setRender(boolean render) {
        externalRender = render;
        updateRender();
    }

    @Override
    public ReadOnlyObjectProperty<BridgeRenderFlag> getRenderFlagProperty() {
        return videoProjector.getRenderFlagProperty();
    }

    private void updateRender() {
        boolean render = externalRender && internalRender;
        videoProjector.setRender(render);
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
        currentMedia = toPlay;
        videoProjector.getPlayer().media().play(toPlay.getAbsolutePath());
        videoProjector.getPlayer().controls().setRepeat(true);

        playing = true;
    }

    public void stopBackground() {
        internalRender = false;
        updateRender();
        playing = false;
        videoProjector.getPlayer().controls().stop();
    }

    @Override
    public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
        internalRender = false;
        updateRender();
    }

    @Override
    public void opening(MediaPlayer mediaPlayer) {

    }

    @Override
    public void buffering(MediaPlayer mediaPlayer, float newCache) {

    }

    @Override
    public void playing(MediaPlayer mediaPlayer) {

    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {
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

    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long newTime) {

    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {

    }

    @Override
    public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {

    }

    @Override
    public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {

    }

    @Override
    public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {

    }

    @Override
    public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {

    }

    @Override
    public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {

    }

    @Override
    public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
        internalRender = true;
        updateRender();
    }

    @Override
    public void scrambledChanged(MediaPlayer mediaPlayer, int newScrambled) {

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
    public void corked(MediaPlayer mediaPlayer, boolean corked) {

    }

    @Override
    public void muted(MediaPlayer mediaPlayer, boolean muted) {

    }

    @Override
    public void volumeChanged(MediaPlayer mediaPlayer, float volume) {

    }

    @Override
    public void audioDeviceChanged(MediaPlayer mediaPlayer, String audioDevice) {

    }

    @Override
    public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {

    }

    @Override
    public void error(MediaPlayer mediaPlayer) {

    }

    @Override
    public void mediaPlayerReady(MediaPlayer mediaPlayer) {

    }
}
