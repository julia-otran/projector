package dev.juhouse.projector.projection.models;

import lombok.Data;
import dev.juhouse.projector.models.WindowConfig;

import java.util.List;

@Data
public class VirtualScreen {
    public static final String MAIN_SCREEN_ID = "main";
    public static final String MAIN_CHROMA_SCREEN_ID = "main-chroma";
    public static final String CHROMA_SCREEN_ID = "chroma";

    private String virtualScreenId;
    private int width;
    private int height;

    private List<WindowConfig> windows;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isMainScreen() {
        return MAIN_SCREEN_ID.equalsIgnoreCase(virtualScreenId) || MAIN_CHROMA_SCREEN_ID.equalsIgnoreCase(virtualScreenId);
    }

    public boolean isChromaScreen() {
        return virtualScreenId.contains(CHROMA_SCREEN_ID);
    }
}
