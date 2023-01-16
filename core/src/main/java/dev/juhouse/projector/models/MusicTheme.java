package dev.juhouse.projector.models;

import javafx.scene.image.ImageView;
import lombok.Data;

import java.io.File;

@Data
public class MusicTheme {
    private File videoFile;
    private File imageFile;
    private ImageView image;
}
