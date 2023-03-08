package dev.juhouse.projector;

import java.io.File;

import static dev.juhouse.projector.utils.ResourceManager.unpackResource;

public class Main {

    private static void loadNativeLib() {
        unpackResource("bin\\glfw3.dll");

        File libExportFile = unpackResource("bin\\LibRender.lib");
        System.load(libExportFile.toString());
    }

    public static void main(String[] args) {
        loadNativeLib();
        Projector.main(args);
    }
}
