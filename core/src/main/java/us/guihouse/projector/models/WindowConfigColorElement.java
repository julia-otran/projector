package us.guihouse.projector.models;

import lombok.Data;

@Data
public class WindowConfigColorElement {
    private float r;
    private float g;
    private float b;

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }
}
