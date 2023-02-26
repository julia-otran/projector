/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;

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

    // TODO: Need a thread to update bridge

    private final CanvasDelegate delegate;

    private SwingNode node;
    private Pane container;
    private WebView webView;
    private JFXPanel panel;

    public ProjectionWebView(CanvasDelegate delegate) {
        this.delegate = delegate;
    }

    public void rebuildLayout() {
        int width = delegate.getMainWidth();
        int height = delegate.getMainHeight();

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

    @Override
    public void setRender(boolean render) {

    }

    public WebView getWebView() {
        return webView;
    }

    public Node getNode() {
        return container;
    }
}
