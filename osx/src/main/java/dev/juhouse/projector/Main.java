package dev.juhouse.projector;

import dev.juhouse.projector.utils.VlcPlayerFactory;

import java.io.File;

import static dev.juhouse.projector.utils.ResourceManager.unpackResource;

public class Main {
    private static void loadNativeLib() {
        File libExportFile = unpackResource("/libProjector.jnilib", "ProjectorLib", "libProjector.jnilib");
        System.load(libExportFile.toString());
    }

    public static void main(String[] args) {
        VlcPlayerFactory.init();
        loadNativeLib();
        Projector.main(args);
    }
}
