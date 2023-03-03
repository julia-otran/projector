/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;

import javax.swing.*;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionWebView implements Projectable, Runnable {
    private final CanvasDelegate delegate;
    private WebView webView;
    
    private boolean render;

    private boolean snapshotRendered;

    private Thread bufferUpdateThread;

    private final Object renderSync = new Object();

    private WritableImage snapshotImage;

    private IntBuffer snapshotBuffer;

    private int width;

    private int height;
    
    public ProjectionWebView(CanvasDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void init() {
        if (webView == null) {
            webView = new WebView();
        }

        rebuild();
    }

    @Override
    public void finish() {
        setRender(false);
    }

    @Override
    public void rebuild() {
        boolean oldRender = render;

        int width = delegate.getMainWidth();
        int height = delegate.getMainHeight();

        Platform.runLater(() -> {
            setRender(false);

            this.width = width;
            this.height = height;

            snapshotBuffer = IntBuffer.allocate(width * height);
            snapshotImage = new WritableImage(width, height);

            webView.setPrefWidth(width);
            webView.setPrefHeight(height);
            webView.setMinWidth(width);
            webView.setMinHeight(height);

            setRender(oldRender);
        });
    }

    @Override
    public void setRender(boolean render) {
        this.render = render;
        
        if (render) {
            bufferUpdateThread = new Thread(this);
            bufferUpdateThread.start();
        } else if (bufferUpdateThread != null) {
            try {
                bufferUpdateThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        delegate.getBridge().setRenderWebViewBuffer(render);
    }   

    public WebView getWebView() {
        return webView;
    }
    
    @Override
    public void run() {
        snapshotRendered = false;

        while (render) {
            Platform.runLater(() -> {
                synchronized (renderSync) {
                    webView.snapshot(null, snapshotImage);
                    snapshotRendered = true;
                    renderSync.notifyAll();
                }
            });

            synchronized (renderSync) {
                try {
                    if (!snapshotRendered) {
                        renderSync.wait(100);
                    }

                    snapshotRendered = false;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (snapshotImage != null) {
                snapshotImage.getPixelReader().getPixels(0, 0, width, height, WritablePixelFormat.getIntArgbInstance(), snapshotBuffer.array(), 0, width);
                delegate.getBridge().setWebViewBuffer(snapshotBuffer.array(), width, height);
            }
        }
    }
}
