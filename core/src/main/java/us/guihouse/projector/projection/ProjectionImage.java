/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapter;
import us.guihouse.projector.projection.glfw.RGBImageCopy;
import us.guihouse.projector.projection.models.BackgroundProvide;
import us.guihouse.projector.projection.models.VirtualScreen;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author guilherme
 */
public class ProjectionImage implements Projectable {

    protected final CanvasDelegate canvasDelegate;

    private final Color bgColor;

    private final ConcurrentHashMap<String, Rectangle> scales = new ConcurrentHashMap<>();

    private BufferedImage image;

    private boolean cropBackground;
    private BackgroundProvide model;

    private final HashMap<String, Integer> texes = new HashMap<>();

    ProjectionImage(CanvasDelegate canvasDelegate) {
        this(canvasDelegate, new Color(0, 0, 0));
    }

    public ProjectionImage(CanvasDelegate canvasDelegate, Color bgColor) {
        this.canvasDelegate = canvasDelegate;
        this.bgColor = bgColor;
    }

    public boolean isEmpty() {
        return image == null || scales.isEmpty();
    }

    @Override
    public void paintComponent(GLFWGraphicsAdapter g, VirtualScreen vs) {
        if (isEmpty()) {
            return;
        }

        Integer tex = texes.get(vs.getVirtualScreenId());

        if (tex == null) {
            return;
        }

        Rectangle position = scales.get(vs.getVirtualScreenId());

        if (position == null) {
            return;
        }

        g.setColor(bgColor);
        g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

        float alpha = g.getAlpha();

        g.getProvider().enqueueForDraw(() -> {
            GL11.glEnable(GL11.GL_BLEND);
            GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            GL11.glPushMatrix();
            g.adjustOrtho();
            g.updateAlpha(alpha);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

            double x = position.getX();
            double y = position.getY();
            double maxX = position.getMaxX();
            double maxY = position.getMaxY();

            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2d(0, 0);
            GL11.glVertex2d(x, y);

            GL11.glTexCoord2d(0, 1);
            GL11.glVertex2d(x, maxY);

            GL11.glTexCoord2d(1, 1);
            GL11.glVertex2d(maxX, maxY);

            GL11.glTexCoord2d(1, 0);
            GL11.glVertex2d(maxX, y);

            GL11.glEnd();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        });

    }

    @Override
    public void rebuildLayout() {
        scales.clear();

        if (model == null) {
            return;
        }

        if (model.getStaticBackground() != null) {
            image = model.getStaticBackground();
            scaleBackground(image);
            generateTexes();
        }
    }

    private void generateTexes() {
        BufferedImage image = this.image;

        canvasDelegate.getVirtualScreens().forEach(vs -> {
            canvasDelegate.runOnProvider(vs, provider -> {
                Integer tex = texes.remove(vs.getVirtualScreenId());

                if (tex != null) {
                    provider.freeTex(tex);
                }

                tex = provider.dequeueTex();

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                int width = image.getWidth();
                int height = image.getHeight();

                int size = width * height * 4;
                ByteBuffer buffer = BufferUtils.createByteBuffer(size);
                RGBImageCopy.copyImageToBuffer(image, buffer, true);

                GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                texes.put(vs.getVirtualScreenId(), tex);
            });
        });
    }

    private void scaleBackground(BufferedImage img) {
        scales.clear();

        canvasDelegate.getVirtualScreens()
            .forEach(vs -> {
                double imgW = img.getWidth();
                double imgH = img.getHeight();

                int width = vs.getWidth();
                int height = vs.getHeight();

                if (width == 0 || height == 0) {
                    return;
                }

                double scaleX = width / imgW;
                double scaleY = height / imgH;
                double backgroundScale;

                if (cropBackground) {
                    backgroundScale = Math.max(scaleX, scaleY);
                } else {
                    backgroundScale = Math.min(scaleX, scaleY);
                }

                int newW = (int) Math.round(imgW * backgroundScale);
                int newH = (int) Math.round(imgH * backgroundScale);

                int x = (width - newW) / 2;
                int y = (height - newH) / 2;

                scales.put(vs.getVirtualScreenId(), new Rectangle(x, y, newW, newH));
            });
    }

    @Override
    public void init() {
        rebuildLayout();
    }

    @Override
    public void finish() {

    }

    public boolean getCropBackground() {
        return cropBackground;
    }

    public void setCropBackground(boolean cropBackground) {
        if (this.cropBackground != cropBackground) {
            this.cropBackground = cropBackground;
            rebuildLayout();
        }
    }

    public BackgroundProvide getModel() {
        return model;
    }

    public void setModel(BackgroundProvide model) {
        this.model = model;
        rebuildLayout();
    }
}
