package us.guihouse.projector.projection;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;

import java.awt.*;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.List;

import static us.guihouse.projector.utils.FilePaths.PROJECTOR_BACKGROUND_VIDEOS;

public class ProjectionBackgroundVideo implements Projectable {
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
        videoProjectors[0].getPlayer().addMediaPlayerEventListener(new Player0Callbacks());

        videoProjectors[1].init();
        videoProjectors[1].getPlayer().mute(true);
        videoProjectors[1].getPlayer().addMediaPlayerEventListener(new Player1Callbacks());

        loadMedia();
    }

    @Override
    public void finish() {
        videoProjectors[0].finish();
        videoProjectors[1].finish();
    }

    public void loadMedia() {
        String[] pathNames = PROJECTOR_BACKGROUND_VIDEOS.toFile().list();

        if (pathNames != null) {
            media.clear();

            for (String fileStr : pathNames) {
                File file = FileSystems.getDefault().getPath(PROJECTOR_BACKGROUND_VIDEOS.toFile().getAbsolutePath(), fileStr).toFile();

                if (file.isFile() && file.canRead()) {
                    media.add(file);
                    videoProjectors[0].getPlayer().prepareMedia(file.getAbsolutePath());
                    videoProjectors[1].getPlayer().prepareMedia(file.getAbsolutePath());
                }
            }
        }
    }

    public void startBackground(Integer musicId) {
        if (!musicMap.containsKey(musicId)) {
            int id = Math.abs(random.nextInt()) % media.size();
            musicMap.put(musicId, media.get(id));
        }

        File toPlay = musicMap.get(musicId);

        if (toPlay != null) {
            if (toPlay.equals(currentMedia)) {
                if (!playing) {
                    playMedia(toPlay.getAbsolutePath());
                }
            } else {
                currentMedia = toPlay;
                playMedia(toPlay.getAbsolutePath());
            }
        }
    }

    private void playMedia(String absolutePath) {
        stopBackground();
        videoProjectors[0].setRender(true);
        playing = true;
        currentPlayer = 0;
        videoProjectors[0].getPlayer().playMedia(absolutePath);
        videoProjectors[1].getPlayer().playMedia(absolutePath);
    }

    public void stopBackground() {
        videoProjectors[0].setRender(false);
        videoProjectors[1].setRender(false);
        videoProjectors[0].getPlayer().stop();
        videoProjectors[1].getPlayer().stop();
        playing = false;
    }

    public class Player0Callbacks implements MediaPlayerEventListener {

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
            if (currentPlayer == 0) {
                videoProjectors[0].setRender(true);
                videoProjectors[1].setRender(false);
                videoProjectors[1].getPlayer().setPosition(0);
            } else {
                mediaPlayer.pause();
            }
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
            currentPlayer = 1;
            videoProjectors[1].getPlayer().play();
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

    public class Player1Callbacks implements MediaPlayerEventListener {

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
            if (currentPlayer == 1) {
                videoProjectors[0].setRender(false);
                videoProjectors[1].setRender(true);
                videoProjectors[0].getPlayer().setPosition(0);
            } else {
                mediaPlayer.pause();
            }
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
            currentPlayer = 0;
            videoProjectors[0].getPlayer().play();
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
}
