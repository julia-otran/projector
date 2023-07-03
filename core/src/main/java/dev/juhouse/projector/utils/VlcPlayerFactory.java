package dev.juhouse.projector.utils;

import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import dev.juhouse.projector.other.OsCheck;

import java.util.ArrayList;
import java.util.List;

public class VlcPlayerFactory {
    private static MediaPlayerFactory factory;

    public static void init() {
        List<String> vlcArgs = new ArrayList<>();

        if (OsCheck.getOperatingSystemType().equals(OsCheck.OSType.MacOS)) {
            // Precisa setar a ENV VLC_PLUGIN_PATH com valor /Applications/VLC.app/Contents/MacOS/plugins
            NativeLibrary.addSearchPath("vlc", "/Applications/VLC.app/Contents/MacOS/lib");
            NativeLibrary.addSearchPath("vlccore", "/Applications/VLC.app/Contents/MacOS/lib");
            NativeLibrary.getInstance("vlccore");
        }

        vlcArgs.add("--no-autoscale");
        vlcArgs.add("--swscale-mode=4");
        vlcArgs.add("--avcodec-fast");
        vlcArgs.add("--avcodec-hurry-up");
        vlcArgs.add("--avcodec-skip-idct=4");
        vlcArgs.add("--quiet-synchro");
        vlcArgs.add("--drop-late-frames");
        vlcArgs.add("--skip-frames");
        vlcArgs.add("--no-spu");
        vlcArgs.add("--no-osd");
        vlcArgs.add("--text-renderer=none");
        vlcArgs.add("--clock-synchro=0");
        vlcArgs.add("--no-lua");
        vlcArgs.add("--live-caching=100");
        vlcArgs.add("--no-video-title-show");
        vlcArgs.add("--no-snapshot-preview");

        factory = new MediaPlayerFactory(vlcArgs);
    }

    public static void finish() {
        factory.release();
    }

    public static MediaPlayerFactory getFactory() {
        return factory;
    }
}
