/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;

import dev.juhouse.projector.projection.models.VirtualScreen;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;

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
    private final HashMap<String, AffineTransform> transforms = new HashMap<>();

    public ProjectionWebView(CanvasDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, vs.getWidth(), vs.getHeight());

        AffineTransform old = g.getTransform();

        AffineTransform dst = g.getTransform();
        dst.concatenate(transforms.get(vs.getVirtualScreenId()));
        g.setTransform(dst);

        panel.paint(g);

        g.setTransform(old);
    }

    @Override
    public void rebuildLayout() {
        int width = delegate.getMainWidth();
        int height = delegate.getMainHeight();

        transforms.clear();

        delegate.getVirtualScreens().forEach(vs -> {
            float scaleX = vs.getWidth() / (float) width;
            float scaleY = vs.getHeight() / (float) height;

            float scale = Math.min(scaleX, scaleY);

            int scaledWidth = Math.round(scale * width);
            int scaledHeight = Math.round(scale * height);

            int x = (vs.getWidth() - scaledWidth) / 2;
            int y = (vs.getHeight() - scaledHeight) / 2;

            AffineTransform t = new AffineTransform();
            t.translate(x, y);
            t.scale(scale, scale);

            transforms.put(vs.getVirtualScreenId(), t);
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

    }

    public WebView getWebView() {
        return webView;
    }

    public Node getNode() {
        return container;
    }
}
