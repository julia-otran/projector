package us.guihouse.projector.projection;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;

public class ProjectionBackgroundVideoLoop implements MediaPlayerEventListener {
    private final int playerIndex;
    private final LoopCallback callback;

    public interface LoopCallback {
        void positionChanged(MediaPlayer mediaPlayer, int playerIndex, float position);
        void playing(MediaPlayer mediaPlayer, int playerIndex);
        void finished(MediaPlayer mediaPlayer, int playerIndex);
    }

    public ProjectionBackgroundVideoLoop(int playerIndex, LoopCallback callback) {
        this.playerIndex = playerIndex;
        this.callback = callback;
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
        callback.playing(mediaPlayer, playerIndex);
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
        callback.finished(mediaPlayer, playerIndex);
    }

    @Override
    public void timeChanged(MediaPlayer mediaPlayer, long l) {
    }

    @Override
    public void positionChanged(MediaPlayer mediaPlayer, float v) {
        callback.positionChanged(mediaPlayer, playerIndex, v);
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
