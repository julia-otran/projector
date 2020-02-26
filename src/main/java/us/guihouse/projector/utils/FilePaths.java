package us.guihouse.projector.utils;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class FilePaths {
    public static final Path PROJECTOR_DATA_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector");

    public static final String PROJECTOR_WINDOW_CONFIG_FILE_NAME = "window-configs.json";
    public static final Path PROJECTOR_WINDOW_CONFIG_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", PROJECTOR_WINDOW_CONFIG_FILE_NAME);
    public static final File PROJECTOR_WINDOW_CONFIG_FILE = PROJECTOR_WINDOW_CONFIG_PATH.toFile();

    public static final Path PROJECTOR_BACKGROUND_VIDEOS = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", "Background Videos");
    public static final Path PROJECTOR_BACKGROUND_VIDEO_THUMBS = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", "Background Videos", "thumbs");
}
