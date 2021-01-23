package us.guihouse.projector.projection.glfw;

import org.lwjgl.opengl.GL20;
import us.guihouse.projector.models.WindowConfigBlackLevelAdjust;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GLFWBlackLevelAdjust implements GLFWDrawer {
    private static class PointF {
        float x;
        float y;
    }

    private final List<PointF> points = new ArrayList<>();
    private float offset;

    public void init(Rectangle bounds, WindowConfigBlackLevelAdjust settings) {
        if (settings != null &&
                settings.getPoints() != null &&
                settings.getPoints().size() > 2) {

            for (Point p : settings.getPoints()) {
                PointF point = new PointF();
                point.x = ((p.x * 2) / (float) bounds.width) - 1.0f;
                point.y = ((p.y * 2) / (float) bounds.height) - 1.0f;
                points.add(point);
            }

            offset = settings.getOffset() / (float) 255;
        }
    }

    @Override
    public void draw() {
        if (points.size() > 0) {
            GL20.glEnable(GL20.GL_BLEND);
            GL20.glEnable(GL20.GL_COLOR_MATERIAL);
            GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glColor4f(1.0f, 1.0f, 1.0f, offset);
            GL20.glBegin(GL20.GL_POLYGON);
            for (PointF point : points) {
                GL20.glVertex3f(point.x, point.y, 0.0f);
            }
            GL20.glEnd();
            GL20.glDisable(GL20.GL_BLEND);
            GL20.glDisable(GL20.GL_COLOR_MATERIAL);
        }
    }

    void finish() {
        points.clear();
    }
}
