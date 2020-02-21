package us.guihouse.projector.models;

import java.util.List;
import lombok.Data;

@Data
public class WindowConfig {

    private String displayId;

    private float blackLevelPadding;
    private int blackLevelX;
    private int blackLevelY;

    private int x;
    private int y;
    private int width;
    private int height;

    private int bgFillX;
    private int bgFillY;
    private int bgFillWidth;
    private int bgFillHeight;

    private List<WindowConfigHelpLine> helpLines;

    private List<WindowConfigBlend> blends;

    private double scaleX;
    private double scaleY;

    private double shearX;
    private double shearY;

    private double rotate;
}
