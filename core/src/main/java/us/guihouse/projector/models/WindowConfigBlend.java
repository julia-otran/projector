package us.guihouse.projector.models;

import lombok.Data;

@Data
public class WindowConfigBlend {
    private int x;
    private int y;
    private int width;
    private int height;
    private int direction;
    private int id;
    private Boolean useCurve;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
