package dev.juhouse.projector.models;

import lombok.Data;

@Data
public class WindowConfigWhiteBalance {
    // 0.0 to 1.0
    // neutral value 0.0
    private WindowConfigColorElement bright;

    // 0.0 to 2.0
    // neutral value 1.0
    private WindowConfigColorElement exposure;

    public WindowConfigColorElement getBright() {
        return bright;
    }

    public WindowConfigColorElement getExposure() {
        return exposure;
    }
}
