package us.guihouse.projector.utils;

import us.guihouse.projector.models.WindowConfigBlend;

import java.awt.image.BufferedImage;

public class BlendGenerator {
    public static BufferedImage makeBlender(WindowConfigBlend blend) {

        BufferedImage img = new BufferedImage(blend.getWidth(), blend.getHeight(), BufferedImage.TYPE_INT_ARGB);

        switch (blend.getDirection()) {
            case 1:
                float step1 = 255.0F / (blend.getWidth() - 1);

                for (int x = 0; x < blend.getWidth(); x++) {
                    int color = Math.round(step1 * (blend.getWidth() - x - 1)) << 24;

                    for (int y = 0; y < blend.getHeight(); y++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
            case 2:
                float step2 = 255.0F / (blend.getHeight() - 1);

                for (int y = 0; y < blend.getHeight(); y++) {
                    int color = Math.round(step2 * (blend.getHeight() - y - 1)) << 24;

                    for (int x = 0; x < blend.getHeight(); x++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
            case 3:
                float step3 = 255.0F / (blend.getHeight() - 1);

                for (int y = 0; y < blend.getHeight(); y++) {
                    int color = Math.round(step3 * y) << 24;

                    for (int x = 0; x < blend.getHeight(); x++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
            default:
                float step = 255.0F / (blend.getWidth() - 1);

                for (int x = 0; x < blend.getWidth(); x++) {
                    int color = Math.round(step * x) << 24;

                    for (int y = 0; y < blend.getHeight(); y++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
        }

        return img;
    }
}
