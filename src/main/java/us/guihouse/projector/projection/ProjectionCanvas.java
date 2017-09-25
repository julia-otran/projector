/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Font;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.projection.text.WrapperFactory;

/**
 *
 * @author guilherme
 */
public class ProjectionCanvas implements ProjectionManager {

    private final CanvasDelegate delegate;
    private final ProjectionBackground background;
    private final ProjectionLabel label;
    private Projectable currentProjectable;

    private final List<Projectable> initializeList;

    ProjectionCanvas(CanvasDelegate delegate) {
        this.delegate = delegate;
        this.initializeList = new ArrayList<>();

        background = new ProjectionBackground(delegate);
        initializeList.add(background);

        label = new ProjectionLabel(delegate);
        initializeList.add(label);
    }

    public void init() {
        initializeList.forEach(p -> p.init());
    }

    public void finish() { initializeList.forEach(p -> p.finish()); }

    protected void paintComponent(Graphics2D g) {
        if (currentProjectable == null) {
            background.paintComponent(g);
            label.paintComponent(g);
        } else {
            currentProjectable.paintComponent(g);
        }
    }

    @Override
    public void setText(WrappedText text) {
        label.setText(text);
    }

    @Override
    public Font getTextFont() {
        return label.getFont();
    }

    @Override
    public void setTextFont(Font font) {
        label.setFont(font);
    }

    @Override
    public void addTextWrapperChangeListener(TextWrapperFactoryChangeListener wrapperChangeListener) {
        label.addWrapperChangeListener(wrapperChangeListener);
    }

    @Override
    public WrapperFactory getWrapperFactory() {
        return label.getWrapperFactory();
    }

    @Override
    public void setBackgroundImageFile(File selectedFile) {
        background.setImage(selectedFile);
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        delegate.setFullScreen(fullScreen);
    }

    @Override
    public void setCropBackground(boolean selected) {
        background.setCropBackground(selected);
    }

    @Override
    public ProjectionWebView createWebView() {
        ProjectionWebView wv = new ProjectionWebView(delegate);
        initializeList.add(wv);
        wv.init();
        return wv;
    }

    @Override
    public void setProjectable(Projectable webView) {
        this.currentProjectable = webView;
    }

    @Override
    public ProjectionImage createImage() {
        ProjectionImage image = new ProjectionImage(delegate);
        initializeList.add(image);
        image.init();
        return image;
    }

    @Override
    public ProjectionPlayer createPlayer() {
        ProjectionPlayer pl = new ProjectionPlayer(delegate);
        initializeList.add(pl);
        pl.init();
        return pl;
    }
}
