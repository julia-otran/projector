package dev.juhouse.projector.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class ResourceManager {
    private static File tmpDirectory = null;

    public static File unpackResource(String name) {
        if (tmpDirectory == null) {
            try {
                tmpDirectory = Files.createTempDirectory("projector-resources").toFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            tmpDirectory.deleteOnExit();
        }

        InputStream libInputStream = ResourceManager.class.getResourceAsStream(name);

        if (libInputStream != null) {
            File libExportFile;

            try {
                libExportFile = new File(tmpDirectory, name);
                libExportFile.deleteOnExit();

                OutputStream output = FileUtils.openOutputStream(libExportFile);
                IOUtils.copy(libInputStream, output);
                output.close();
                libInputStream.close();

                return libExportFile;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Resource not found: " + name);
    }
}
