/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import java.nio.ByteBuffer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectionWebView implements Projectable, Runnable {
    private final CanvasDelegate delegate;
    private WebView webView;

    private final ReadOnlyObjectWrapper<BridgeRenderFlag> renderFlag;

    private boolean render;

    private boolean snapshotRendered;

    private Thread bufferUpdateThread;

    private WritableImage snapshotImage;

    private ByteBuffer snapshotBuffer;

    private int width;

    private int height;
    
    public ProjectionWebView(CanvasDelegate delegate) {
        this.delegate = delegate;
        this.renderFlag = new ReadOnlyObjectWrapper<>(new BridgeRenderFlag(delegate));
    }

    @Override
    public void init() {
        if (webView == null) {
            webView = new WebView();
        }
    }

    @Override
    public void finish() {
        setRender(false);
    }

    @Override
    public void rebuild() {
        renderFlag.get().applyDefault(BridgeRender::getEnableRenderVideo);

        boolean oldRender = render;

        int width = delegate.getMainWidth();
        int height = delegate.getMainHeight();

        Platform.runLater(() -> {
            setRender(false);

            this.width = width;
            this.height = height;

            snapshotBuffer = ByteBuffer.allocateDirect(width * height * 4);
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

        if (render) {
            delegate.getBridge().setRenderWebViewBuffer(renderFlag.get().getFlagValue());
        } else {
            delegate.getBridge().setRenderWebViewBuffer(BridgeRenderFlag.NO_RENDER);
        }
    }

    public WebView getWebView() {
        return webView;
    }
    
    @Override
    public void run() {
        while (render) {
            snapshotRendered = false;

            Platform.runLater(() -> {
                webView.snapshot(null, snapshotImage);
                snapshotRendered = true;
            });

            do {
                Thread.yield();
            } while (!snapshotRendered && render);

            snapshotImage.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), snapshotBuffer, width * 4);
            delegate.getBridge().setWebViewBuffer(snapshotBuffer, width, height);
        }
    }
}
