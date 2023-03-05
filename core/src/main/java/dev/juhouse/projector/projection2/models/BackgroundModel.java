package dev.juhouse.projector.projection2.models;

import javafx.scene.image.Image;
import lombok.Data;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class BackgroundModel implements BackgroundProvide {
    private File staticBackgroundFile;

    @Override
    public Image getStaticBackground() {
        if (staticBackgroundFile == null) {
            return null;
        }

        Image img = new Image(staticBackgroundFile.toURI().toString());

        if (img.isError()) {
            Logger.getLogger(BackgroundModel.class.getName()).log(Level.SEVERE, img.getException().getMessage(), img.getException());
            return null;
        }

        return img;
    }
}
