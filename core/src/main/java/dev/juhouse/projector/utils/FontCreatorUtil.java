package dev.juhouse.projector.utils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FontCreatorUtil {
    private static Font MONTSERRAT_FONT;

    public static Font getMontserratFont() {
        if (MONTSERRAT_FONT != null) {
            return MONTSERRAT_FONT;
        }

        InputStream fontStream = FontCreatorUtil.class.getClassLoader().getResourceAsStream("fonts/Montserrat-Medium.ttf");

        if (fontStream != null) {
            try {
                MONTSERRAT_FONT = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            } catch (FontFormatException | IOException e) {
                Logger.getLogger(FontCreatorUtil.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        }

        if (MONTSERRAT_FONT == null) {
            Logger.getLogger(FontCreatorUtil.class.getName()).log(Level.WARNING, "Failed loading Montserrat font.");
            MONTSERRAT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        }

        return MONTSERRAT_FONT;
    }

    public static Font createFont(String name, int style, int size) {
        if (MONTSERRAT_FONT.getFamily().equals(name)) {
            return MONTSERRAT_FONT.deriveFont(style).deriveFont(size * 1.0f);
        }

        return new Font(name, style, size);
    }
}
