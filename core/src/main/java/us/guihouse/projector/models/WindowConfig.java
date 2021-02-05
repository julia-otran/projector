package us.guihouse.projector.models;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import lombok.Data;

@Data
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

    private int bgFillX;
    private int bgFillY;
    private int bgFillWidth;
    private int bgFillHeight;

    private List<WindowConfigHelpLine> helpLines;

    private List<WindowConfigBlend> blends;

    private WindowConfigBlackLevelAdjust blackLevelAdjust;
    private WindowConfigWhiteBalance whiteBalance;
    private WindowConfigColorBalance colorBalance;

    private double scaleX;
    private double scaleY;

    private double shearX;
    private double shearY;

    private double rotate;

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
