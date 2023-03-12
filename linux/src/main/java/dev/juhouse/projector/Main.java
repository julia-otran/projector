package dev.juhouse.projector;

import java.io.File;
import static dev.juhouse.projector.utils.ResourceManager.unpackResource;

public class Main {
    private static void loadNativeLib() {
        File libExportFile = unpackResource("/librender.so");
        System.load(libExportFile.toString());
    }

    public static void main(String[] args) {
        loadNativeLib();
        Projector.main(args);
    }
}
