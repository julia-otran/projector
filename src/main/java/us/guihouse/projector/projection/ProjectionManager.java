/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.projection.text.WrapperFactory;
import java.io.File;
import java.awt.Font;

/**
 *
 * @author 15096134
 */
public interface ProjectionManager {

    void setText(WrappedText text);

    Font getTextFont();

    void setTextFont(Font font);

    void addTextWrapperChangeListener(TextWrapperFactoryChangeListener wrapperChangeListener);
    
    ProjectionWebView createWebView();
    ProjectionImage createImage();
    
    void setProjectable(Projectable webView);

    public WrapperFactory getWrapperFactory();

    public void setBackgroundImageFile(File selectedFile);
    
    public void setFullScreen(boolean fullScreen);

    public void setCropBackground(boolean selected);

}
