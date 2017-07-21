/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;

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
    private Dimension maxSize;
    
    public ProjectionWebView(CanvasDelegate delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void paintComponent(Graphics2D g) {
        panel.paint(g);
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return delegate;
    }

    @Override
    public void rebuildLayout() {
        int width = delegate.getWidth();
        int height = delegate.getHeight();
        
        panel.setBounds(0, 0, width, height);
        panel.setPreferredSize(new Dimension(width, height));
    }

    @Override
    public void init() {
        if (webView == null) {
            this.webView = new WebView();
            this.panel = new JFXPanel();
            panel.setScene(new Scene(webView));
            this.node = new SwingNode();
            node.setContent(panel);
            this.container = new Pane();
            container.getChildren().add(node);
        }
        
        rebuildLayout();
    }

    public Dimension getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Dimension maxSize) {
        this.maxSize = maxSize;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }
    
    public Node getNode() {
        return container;
    }
    
    public void updateNodeScale() {
        int width = panel.getWidth();
        int height = panel.getHeight();
        
        double oldw = node.getLayoutBounds().getWidth();
        double oldh = node.getLayoutBounds().getHeight();
        
        double ratio = height / (double) width;
        
        double neww = oldw;
        double newh = ratio * neww;
        
        if (Double.compare(newh, oldh) > 0) {
            newh = oldh;
            neww = newh / ratio;
        }
        
        double scalex = neww / (double) width;
        double scaley = newh / (double) height;
        
        node.setScaleX(scalex);
        node.setScaleY(scaley);
    }
}