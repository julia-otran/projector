package dev.juhouse.projector.models;

import lombok.Data;

@Data
public class WindowConfigColorBalance {
    // from -1.0 to 1.0
    // neutral value 0.0
    private WindowConfigColorElement shadows;
    private WindowConfigColorElement midtones;
    private WindowConfigColorElement highlights;
    private boolean preserveLuminosity;

    public boolean isPreserveLuminosity() {
        return preserveLuminosity;
    }

    public WindowConfigColorElement getShadows() {
        return shadows;
    }

    public WindowConfigColorElement getMidtones() {
        return midtones;
    }

    public WindowConfigColorElement getHighlights() {
        return highlights;
    }
}
