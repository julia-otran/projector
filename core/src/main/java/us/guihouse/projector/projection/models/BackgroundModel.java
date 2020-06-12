package us.guihouse.projector.projection.models;

import lombok.Data;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Data
public class BackgroundModel implements BackgroundProvide {
    private File staticBackgroundFile;

    @Override
    public BufferedImage getStaticBackground() {
        if (staticBackgroundFile == null) {
            return null;
        }

        try {
            return ImageIO.read(staticBackgroundFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
