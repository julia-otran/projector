package us.guihouse.projector.models;

import lombok.Data;

import java.awt.Point;
import java.util.List;

@Data
public class WindowConfigBlackLevelAdjust {
    private List<Point> points;
    private int offset;
}
