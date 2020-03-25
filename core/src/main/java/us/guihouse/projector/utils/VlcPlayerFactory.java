package us.guihouse.projector.utils;

import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import us.guihouse.projector.other.OsCheck;

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

        if (OsCheck.getOperatingSystemType().equals(OsCheck.OSType.Windows)) {
            // Precisa setar a ENV VLC_PLUGIN_PATH com valor C:\VLC\plugins
            NativeLibrary.addSearchPath("libvlc", "C:\\VLC");
        }

        factory = new MediaPlayerFactory(vlcArgs);
    }

    public static void finish() {

    }

    public static MediaPlayerFactory getFactory() {
        return factory;
    }
}
