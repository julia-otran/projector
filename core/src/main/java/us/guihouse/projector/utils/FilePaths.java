package us.guihouse.projector.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class FilePaths {
    public static final Path PROJECTOR_DATA_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Projector");

    public static final String PROJECTOR_WINDOW_CONFIG_DIR_NAME = "Window Configs";
    public static final Path PROJECTOR_WINDOW_CONFIG_PATH = FileSystems.getDefault().getPath(PROJECTOR_DATA_PATH.toString(), PROJECTOR_WINDOW_CONFIG_DIR_NAME);
    public static final Pattern ALLOWED_WINDOW_CONFIG_FILE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]+$");

    public static final Path PROJECTOR_BACKGROUND_VIDEOS = FileSystems.getDefault().getPath(PROJECTOR_DATA_PATH.toString(), "Background Videos");
    public static final Path PROJECTOR_BACKGROUND_VIDEO_THUMBS = FileSystems.getDefault().getPath(PROJECTOR_DATA_PATH.toString(), "Background Videos", "thumbs");
}
