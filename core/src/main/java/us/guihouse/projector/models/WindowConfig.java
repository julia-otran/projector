package us.guihouse.projector.models;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WindowConfig {

    private String displayId;
    private String virtualScreenId;
    private Rectangle displayBounds;
    private boolean project;

    private int x;
    private int y;
    private int width;
    private int height;

    // Reloadable Fields
    private List<WindowConfigHelpLine> helpLines;

    private List<WindowConfigBlend> blends;

    private WindowConfigBlackLevelAdjust blackLevelAdjust;
    private WindowConfigWhiteBalance whiteBalance;
    private WindowConfigColorBalance colorBalance;

    public String getDisplayId() {
        return displayId;
    }

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

    public List<WindowConfigBlend> getBlends() {
        return blends;
    }

    public WindowConfigBlackLevelAdjust getBlackLevelAdjust() {
        return blackLevelAdjust;
    }

    public WindowConfigWhiteBalance getWhiteBalance() {
        return whiteBalance;
    }

    public WindowConfigColorBalance getColorBalance() {
        return colorBalance;
    }

    public List<WindowConfigHelpLine> getHelpLines() {
        return helpLines;
    }

    public boolean allowQuickReload(WindowConfig other) {
        return
                Objects.equals(displayId, other.displayId) &&
                        Objects.equals(virtualScreenId, other.virtualScreenId) &&
                        Objects.equals(displayBounds, other.displayBounds) &&
                        project == other.project &&
                        x == other.x &&
                        y == other.y &&
                        width == other.width &&
                        height == other.height;

    }
}
