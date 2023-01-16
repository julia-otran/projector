package dev.juhouse.projector.utils;

import dev.juhouse.projector.models.WindowConfigBlend;

import java.awt.image.BufferedImage;

public class BlendGenerator {
    private static int curve(float x, WindowConfigBlend blend) {
        if (blend.getUseCurve() == null || !blend.getUseCurve()) {
            return Math.round(x);
        }

        return Math.round(((x * x) / 65025.0f) * 255);
    }

    public static BufferedImage makeBlender(WindowConfigBlend blend) {
        BufferedImage img = new BufferedImage(blend.getWidth(), blend.getHeight(), BufferedImage.TYPE_INT_ARGB);

        switch (blend.getDirection()) {
            case 1:
                float step1 = 255.0F / (blend.getWidth() - 1);

                for (int x = 0; x < blend.getWidth(); x++) {
                    int color = curve(step1 * (blend.getWidth() - x - 1), blend) << 24;

                    for (int y = 0; y < blend.getHeight(); y++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
            case 2:
                float step2 = 255.0F / (blend.getHeight() - 1);

                for (int y = 0; y < blend.getHeight(); y++) {
                    int color = curve(step2 * (blend.getHeight() - y - 1), blend) << 24;

                    for (int x = 0; x < blend.getWidth(); x++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
            case 3:
                float step3 = 255.0F / (blend.getHeight() - 1);

                for (int y = 0; y < blend.getHeight(); y++) {
                    int color = curve(step3 * y, blend) << 24;

                    for (int x = 0; x < blend.getWidth(); x++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
            default:
                float step = 255.0F / (blend.getWidth() - 1);

                for (int x = 0; x < blend.getWidth(); x++) {
                    int color = curve(step * x, blend) << 24;

                    for (int y = 0; y < blend.getHeight(); y++) {
                        img.setRGB(x, y, color);
                    }
                }
                break;
        }

        return img;
    }
}
