/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
    private Projectable currentWebView;
    
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

    protected void paintComponent(Graphics g) {
        if (currentWebView == null) {
            background.paintComponent(g);
            label.paintComponent(g);
        } else {
            currentWebView.paintComponent(g);
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
    public TextWrapperFactoryChangeListener getTextWrapperChangeListener() {
        return label.getWrapperChangeListener();
    }

    @Override
    public void setTextWrapperChangeListener(TextWrapperFactoryChangeListener wrapperChangeListener) {
        label.setWrapperChangeListener(wrapperChangeListener);
    }

    @Override
    public WrapperFactory getWrapperFactory() {
        return label.getWrapperFactory();
    }

    @Override
    public void setBackgroundImageFile(File selectedFile) {
        try {
            BufferedImage image = ImageIO.read(selectedFile);
            background.setImage(image);
        } catch (IOException ex) {
            Logger.getLogger(ProjectionCanvas.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        
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
        this.currentWebView = webView;
    }

    @Override
    public ProjectionImage createImage() {
        ProjectionImage image = new ProjectionImage(delegate);
        initializeList.add(image);
        image.init();
        return image;
    }
}
