/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Font;
import java.io.File;

import javafx.beans.property.ReadOnlyProperty;
import us.guihouse.projector.projection.models.BackgroundModel;
import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.projection.text.WrapperFactory;
import us.guihouse.projector.projection.video.ProjectionPlayer;

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

    WrapperFactory getWrapperFactory();

    BackgroundModel getBackgroundModel();

    ReadOnlyProperty<Projectable> projectableProperty();

    void setBackgroundModel(BackgroundModel background);

    public void setFullScreen(boolean fullScreen);

    public void setCropBackground(boolean selected);

    public ProjectionPlayer createPlayer();

    void setAnimateBackground(boolean selected);

    public boolean getDarkenBackground();

    public void setDarkenBackground(boolean darkenBg);

    void stop(Projectable projectable);

    void setMusicForBackground(Integer musicId, File preferred);

    boolean getCropBackground();
}
