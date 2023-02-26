package dev.juhouse.projector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Main {
    private static void loadNativeLib() {
        InputStream libInputStream = Main.class.getResourceAsStream("/librender.so");

        if (libInputStream != null) {
            File libExportFile;

            try {
                libExportFile = File.createTempFile("librender", ".so");
                libExportFile.deleteOnExit();

                OutputStream output = FileUtils.openOutputStream(libExportFile);
                IOUtils.copy(libInputStream, output);
                output.close();
                libInputStream.close();

                System.load(libExportFile.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Failed to find librender.so resource");
        }
    }
    public static void main(String[] args) {
        loadNativeLib();

//        MainTest.main(args);
         Projector.main(args);
    }
}
