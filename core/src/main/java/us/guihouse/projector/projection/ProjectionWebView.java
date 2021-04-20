/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapter;
import us.guihouse.projector.projection.glfw.RGBImageCopy;
import us.guihouse.projector.projection.models.VirtualScreen;

import javax.swing.*;

/**
 *
 * @author guilherme
 */
public class ProjectionWebView implements Projectable {

    private final CanvasDelegate delegate;

    private SwingNode node;
    private Pane container;
    private WebView webView;
    private JFXPanel panel;
    private final HashMap<String, Rectangle> positions = new HashMap<>();
    private final HashMap<String, Integer> texes = new HashMap<>();
    private BufferedImage source;
    private Graphics sourceGraphics;
    private boolean painting = false;

    private final HashMap<String, Integer> texesW = new HashMap<>();
    private final HashMap<String, Integer> texesH = new HashMap<>();

    private boolean finished;

    public ProjectionWebView(CanvasDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void paintComponent(GLFWGraphicsAdapter g, VirtualScreen vs) {
        if (finished) {
            return;
        }

        if (!painting) {
            painting = true;

            SwingUtilities.invokeLater(() -> {
                if (sourceGraphics != null) {
                    panel.paint(sourceGraphics);
                }
                painting = false;
            });
        }

        BufferedImage source = this.source;

        if (source == null) {
            return;
        }

        int width = source.getWidth();
        int height = source.getHeight();

        int buffer = g.getProvider().dequeueGlBuffer();
        GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer);

        GL30.glBufferData(
                GL30.GL_PIXEL_UNPACK_BUFFER,
                (long) width * height * 4,
                GL30.GL_STREAM_DRAW
        );

        ByteBuffer destination = GL30.glMapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, GL30.GL_WRITE_ONLY);

        if (destination != null) {
            RGBImageCopy.copyImageToBuffer(source, destination, true);
        }

        GL30.glUnmapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER);
        GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

        Rectangle position = positions.get(vs.getVirtualScreenId());

        if (position == null) {
            return;
        }

        float alpha = g.getAlpha();

        g.getProvider().enqueueForDraw(() -> {
            GL11.glEnable(GL11.GL_BLEND);
            GL20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            Integer tex = texes.get(vs.getVirtualScreenId());

            if (tex == null) {
                tex = g.getProvider().dequeueTex();

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                texes.put(vs.getVirtualScreenId(), tex);
            }

            GL11.glPushMatrix();
            g.adjustOrtho();
            g.updateAlpha(alpha);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, buffer);

            Integer texW = texesW.get(vs.getVirtualScreenId());
            Integer texH = texesH.get(vs.getVirtualScreenId());

            if (width != texW || height != texH) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
                texesW.put(vs.getVirtualScreenId(), width);
                texesH.put(vs.getVirtualScreenId(), height);
            } else {
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
            }

            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2d(0, 0);
            GL11.glVertex2d(position.getX(), position.getY());

            GL11.glTexCoord2d(0, 1);
            GL11.glVertex2d(position.getX(), position.getMaxY());

            GL11.glTexCoord2d(1, 1);
            GL11.glVertex2d(position.getMaxX(), position.getMaxY());

            GL11.glTexCoord2d(1, 0);
            GL11.glVertex2d(position.getMaxX(), position.getY());

            GL11.glEnd();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL30.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        });

    }

    @Override
    public void rebuildLayout() {
        int width = delegate.getMainWidth();
        int height = delegate.getMainHeight();

        if (sourceGraphics != null) {
            sourceGraphics.dispose();
        }

        source = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        sourceGraphics = source.createGraphics();

        delegate.getVirtualScreens().forEach(vs -> {
            float scaleX = vs.getWidth() / (float) width;
            float scaleY = vs.getHeight() / (float) height;

            float scale = Math.min(scaleX, scaleY);

            int scaledWidth = Math.round(scale * width);
            int scaledHeight = Math.round(scale * height);

            int x = (vs.getWidth() - scaledWidth) / 2;
            int y = (vs.getHeight() - scaledHeight) / 2;

            positions.put(vs.getVirtualScreenId(), new Rectangle(x, y, scaledWidth, scaledHeight));
        });

        Platform.runLater(() -> {
            webView.setPrefWidth(width);
            webView.setPrefHeight(height);
            webView.setMinWidth(width);
            webView.setMinHeight(height);

            node.resize(width, height);

            SwingUtilities.invokeLater(() -> {
                panel.setMinimumSize(new Dimension(width, height));
                panel.setPreferredSize(new Dimension(width, height));

                Component root = panel;

                while (root != null) {
                    root.setMinimumSize(new Dimension(width, height));
                    root.setBounds(0, 0, width, height);
                    root = root.getParent();
                }
            });
        });
    }

    @Override
    public void init() {
        finished = false;

        if (webView == null) {
            webView = new WebView();

            int width = delegate.getMainWidth();
            int height = delegate.getMainHeight();

            Scene scene = new Scene(webView, width, height);

            panel = new JFXPanel() {
                @Override
                public void setBounds(int x, int y, int width, int height) {
                    if (width >= delegate.getMainWidth() || height >= delegate.getMainHeight()) {
                        super.setBounds(x, y, width, height);
                    }
                }
            };

            panel.setScene(scene);

            this.node = new SwingNode() {
                @Override
                public boolean isResizable() {
                    return false;
                }
            };

            node.setContent(panel);

            this.container = new Pane();

            container.setMinWidth(width);
            container.setMinHeight(height);

            container.getChildren().add(node);
        }

        rebuildLayout();
    }

    @Override
    public void finish() {
        finished = true;

        delegate.getVirtualScreens().forEach(vs -> {
            Integer tex = texes.remove(vs.getVirtualScreenId());
            if (tex != null) {
                delegate.runOnProvider(vs, provider -> provider.freeTex(tex));
            }
        });
    }

    public WebView getWebView() {
        return webView;
    }

    public Node getNode() {
        return container;
    }
}
