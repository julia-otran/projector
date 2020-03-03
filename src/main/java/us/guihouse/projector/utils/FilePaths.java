package us.guihouse.projector.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class FilePaths {
    public static final Path PROJECTOR_DATA_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector");

    public static final String PROJECTOR_WINDOW_CONFIG_DIR_NAME = "Window Configs";
    public static final Path PROJECTOR_WINDOW_CONFIG_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", PROJECTOR_WINDOW_CONFIG_DIR_NAME);
    public static final Path PROJECTOR_DEFAULT_WINDOW_CONFIG_FILE = FileSystems.getDefault().getPath(PROJECTOR_WINDOW_CONFIG_PATH.toString(), "configs.json");

    public static final Path PROJECTOR_BACKGROUND_VIDEOS = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", "Background Videos");
    public static final Path PROJECTOR_BACKGROUND_VIDEO_THUMBS = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector", "Background Videos", "thumbs");
}
