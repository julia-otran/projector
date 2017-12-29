package us.guihouse.projector.projection.models;

import javafx.scene.image.Image;
import lombok.Data;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Data
public class BackgroundModel implements BackgroundProvide {
    private Type type;

    private File backgroundFile;
    private File logoFile;
    private File overlayFile;

    private File staticBackgroundFile;

    @Override
    public BufferedImage getBackground() {
        if (backgroundFile == null) {
            return null;
        }

        try {
            return ImageIO.read(backgroundFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public BufferedImage getLogo() {
        if (logoFile == null) {
            return null;
        }

        try {
            return ImageIO.read(logoFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public BufferedImage getOverlay() {
        if (overlayFile == null) {
            return null;
        }

        try {
            return ImageIO.read(overlayFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

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
