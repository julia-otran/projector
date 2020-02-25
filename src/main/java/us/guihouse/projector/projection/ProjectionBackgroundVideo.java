package us.guihouse.projector.projection;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.*;

import static us.guihouse.projector.utils.FilePaths.PROJECTOR_BACKGROUND_VIDEOS;

public class ProjectionBackgroundVideo extends ProjectionVideo implements MediaPlayerEventListener {
    private List<File> media = new ArrayList<>();
    private File currentMedia = null;
    private Map<Integer, File> musicMap = new HashMap<>();
    private Random random = new Random();

    public ProjectionBackgroundVideo(CanvasDelegate delegate) {
        super(delegate, true);
    }

    @Override
    public void init() {
        super.init();
        getPlayer().addMediaPlayerEventListener(this);
        getPlayer().setRepeat(true);
        getPlayer().mute(true);
        loadMedia();
    }

    public void loadMedia() {
        String[] pathNames = PROJECTOR_BACKGROUND_VIDEOS.toFile().list();

        if (pathNames != null) {
            media.clear();

            for (String fileStr : pathNames) {
                File file = FileSystems.getDefault().getPath(PROJECTOR_BACKGROUND_VIDEOS.toFile().getAbsolutePath(), fileStr).toFile();

                if (file.isFile() && file.canRead()) {
                    media.add(file);
                }
            }
        }
    }

    public void startBackground(Integer musicId) {
        if (!musicMap.containsKey(musicId)) {
            int id = random.nextInt() % media.size();
            musicMap.put(musicId, media.get(id));
        }

        File toPlay = musicMap.get(musicId);

        if (toPlay != null && !toPlay.equals(currentMedia)) {
            currentMedia = toPlay;
            getPlayer().playMedia(toPlay.getAbsolutePath());
            setRender(true);
        }
    }

    public void stopBackground() {
        setRender(false);
        getPlayer().stop();
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
    public void timeChanged(MediaPlayer mediaPlayer, long l) {

    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float v) {

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
