/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import java.awt.Font;
import java.io.File;

import dev.juhouse.projector.projection2.countdown.ProjectionCountdown;
import dev.juhouse.projector.projection2.models.BackgroundModel;
import dev.juhouse.projector.projection2.text.WrappedText;
import dev.juhouse.projector.projection2.text.WrapperFactory;
import dev.juhouse.projector.projection2.video.ProjectionPlayer;
import javafx.beans.property.ReadOnlyProperty;

/**
 *
 * @author 15096134
 */
public interface ProjectionManager {
    void init();

    void finish();

    void rebuild();

    void setText(WrappedText text);

    Font getTextFont();

    void setTextFont(Font font);

    void addTextWrapperChangeListener(TextWrapperFactoryChangeListener wrapperChangeListener);

    ProjectionLabel createLabel();

    ProjectionWebView createWebView();

    ProjectionImage createImage();

    ProjectionWindowCapture createWindowCapture();

    void setProjectable(Projectable webView);

    WrapperFactory getWrapperFactory();

    BackgroundModel getBackgroundModel();

    ReadOnlyProperty<Projectable> projectableProperty();

    void setBackgroundModel(BackgroundModel background);

    void setCropBackground(boolean selected);

    ProjectionPlayer createPlayer();

    ProjectionCountdown createCountdown();

    boolean getDarkenBackground();

    void setDarkenBackground(boolean darkenBg);

    void stop(Projectable projectable);

    void setMusicForBackground(Integer musicId, File preferred);

    boolean getCropBackground();

    void addCallback(ProjectionManagerCallbacks callback);

    void removeCallback(ProjectionManagerCallbacks callback);

}
