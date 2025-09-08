package dev.juhouse.projector;

import dev.juhouse.projector.utils.VlcPlayerFactory;

import java.io.File;
import static dev.juhouse.projector.utils.ResourceManager.unpackResource;

public class Main {
    private static void loadNativeLib() {
        File libNDIFile = unpackResource("/libndi.so.6.2.1");
        File libExportFile = unpackResource("/librender.so");
        System.load(libNDIFile.toString());
        System.load(libExportFile.toString());
    }

    public static void main(String[] args) {
        VlcPlayerFactory.init();
        loadNativeLib();
        Projector.main(args);
    }
}
