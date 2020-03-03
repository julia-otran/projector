package us.guihouse.projector.utils;

import javafx.scene.image.ImageView;
import us.guihouse.projector.models.MusicTheme;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import static us.guihouse.projector.utils.FilePaths.PROJECTOR_BACKGROUND_VIDEOS;
import static us.guihouse.projector.utils.FilePaths.PROJECTOR_BACKGROUND_VIDEO_THUMBS;

public class ThemeFinder {
    private static final List<MusicTheme> media = new ArrayList<>();

    public static void loadThemes() {
        media.clear();

        String[] pathNames = PROJECTOR_BACKGROUND_VIDEOS.toFile().list();

        if (pathNames != null) {
            for (String fileStr : pathNames) {
                File file = FileSystems.getDefault().getPath(PROJECTOR_BACKGROUND_VIDEOS.toFile().getAbsolutePath(), fileStr).toFile();
                File thumb = FileSystems.getDefault().getPath(PROJECTOR_BACKGROUND_VIDEO_THUMBS.toFile().getAbsolutePath(), fileStr + ".png").toFile();

                if (file.isFile() && file.canRead() && thumb.isFile() && thumb.canRead()) {
                    MusicTheme t = new MusicTheme();
                    t.setVideoFile(file);
                    t.setImageFile(thumb);

                    ImageView thumbImg = new ImageView(thumb.toURI().toString());
                    thumbImg.setPreserveRatio(true);
                    thumbImg.setFitWidth(200D);
                    thumbImg.setFitHeight(200D);

                    t.setImage(thumbImg);
                    media.add(t);
                }
            }
        }
    }

    public static List<MusicTheme> getThemes() {
        return media;
    }

    public static MusicTheme getThemeByVideoName(String name) {
        return getThemes().stream().filter(t -> t.getVideoFile().getName().equals(name)).findAny().orElse(null);
    }
}
