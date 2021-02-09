package us.guihouse.projector.projection.glfw;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL20;
import us.guihouse.projector.models.WindowConfigBlackLevelAdjust;

import java.awt.*;
import java.nio.FloatBuffer;

public class GLFWBlackLevelAdjust implements GLFWDrawer {

    private float offset;
    private int vertexCount;
    private int vertexBuffer = -1;
    private GLFWVidMode bounds;

    public void init(GLFWVidMode bounds) {
        this.bounds = bounds;
    }

    public void updateConfigs(WindowConfigBlackLevelAdjust settings) {
        if (settings != null &&
                settings.getPoints() != null &&
                settings.getPoints().size() > 2) {

            vertexCount = settings.getPoints().size();

            FloatBuffer coordBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);

            for (Point p : settings.getPoints()) {
                float x = ((p.x * 2) / (float) bounds.width()) - 1.0f;
                float y = ((p.y * 2) / (float) bounds.height()) - 1.0f;
                coordBuffer.put(x);
                coordBuffer.put(y);
            }

            coordBuffer.flip();

            finish();

            vertexBuffer = GL20.glGenBuffers();
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vertexBuffer);
            GL20.glBufferData(GL20.GL_ARRAY_BUFFER, coordBuffer, GL20.GL_STATIC_DRAW);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

            offset = settings.getOffset() / (float) 255;
        }
    }

    @Override
    public void draw() {
        if (vertexCount > 0 && vertexBuffer >= 0) {
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vertexBuffer);

            GL20.glEnable(GL20.GL_BLEND);
            GL20.glEnable(GL20.GL_COLOR_MATERIAL);
            GL20.glEnable(GL20.GL_MULTISAMPLE);
            GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glColor4f(1.0f, 1.0f, 1.0f, offset);

            GL20.glEnableClientState(GL20.GL_VERTEX_ARRAY);
            GL20.glVertexPointer(2, GL20.GL_FLOAT, 0, 0);
            GL20.glDrawArrays(GL20.GL_POLYGON, 0, vertexCount);
            GL20.glDisableClientState(GL20.GL_VERTEX_ARRAY);

            GL20.glDisable(GL20.GL_BLEND);
            GL20.glDisable(GL20.GL_COLOR_MATERIAL);
            GL20.glDisable(GL20.GL_MULTISAMPLE);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        }
    }

    void finish() {
        if (vertexBuffer >= 0) {
            GL20.glDeleteBuffers(vertexBuffer);
        }
    }
}
