package dev.juhouse.projector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
