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

    public static File unpackResource(String resource) {
        return unpackResource(resource, null, resource);
    }

    public static File unpackResource(String resource, String outputName) {
        return unpackResource(resource, null, outputName);
    }

    public static File unpackResource(String resource, String directory, String outputName) {
        if (tmpDirectory == null) {
            try {
                tmpDirectory = Files.createTempDirectory("projector-resources").toFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            tmpDirectory.deleteOnExit();
        }

        File directoryFile = tmpDirectory;

        if (directory != null) {
            directoryFile = new File(tmpDirectory, directory);
            if (!directoryFile.isDirectory() && !directoryFile.mkdirs()) {
                throw new RuntimeException("Cannot mkdir " + directory);
            }
        }

        InputStream libInputStream = ResourceManager.class.getResourceAsStream(resource);

        if (libInputStream != null) {
            File libExportFile;

            try {
                libExportFile = new File(directoryFile, outputName);
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

        System.out.println("Resource not found: " + resource);

        return null;
    }
}
