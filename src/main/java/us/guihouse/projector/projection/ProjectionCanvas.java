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
    private CanvasDelegate delegate;
    private ProjectionBackground background;
    private ProjectionLabel label;

    ProjectionCanvas(CanvasDelegate delegate) {
        this.delegate = delegate;
        background = new ProjectionBackground(delegate);
        label = new ProjectionLabel(delegate);
    }
    
    public void init() {
        background.init();
        label.init();
    }

    protected void paintComponent(Graphics g) {
        background.paintComponent(g);
        label.paintComponent(g);
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
}
