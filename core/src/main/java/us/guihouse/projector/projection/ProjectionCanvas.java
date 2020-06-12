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
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import us.guihouse.projector.other.ProjectorPreferences;
import us.guihouse.projector.projection.models.BackgroundModel;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.projection.text.WrapperFactory;
import us.guihouse.projector.projection.video.ProjectionBackgroundVideo;
import us.guihouse.projector.projection.video.ProjectionPlayer;

/**
 *
 * @author guilherme
 */
public class ProjectionCanvas implements ProjectionManager {

    private final CanvasDelegate delegate;
    private final ProjectionBackground background;
    private final ProjectionLabel label;
    private final ProjectionBackgroundVideo bgVideo;

    private final HashMap<String, PaintableCrossFader> faders = new HashMap<>();

    private final ReadOnlyObjectWrapper<Projectable> currentProjectable = new ReadOnlyObjectWrapper<>();

    private final List<Projectable> initializeList;
    private boolean initialized = false;

    ProjectionCanvas(CanvasDelegate delegate) {
        this.delegate = delegate;
        this.initializeList = new ArrayList<>();

        background = new ProjectionBackground(delegate);
        initializeList.add(background);

        label = new ProjectionLabel(delegate);
        initializeList.add(label);

        bgVideo = new ProjectionBackgroundVideo(delegate);
        initializeList.add(bgVideo);

        setupFader();
    }

    private void setupFader() {
        currentProjectable.addListener((prop, oldVal, newVal) -> updateFaders());
        bgVideo.isRender().addListener((prop, oldVal, newVal) -> updateFaders());
    }

    private void updateFaders() {
        faders.forEach((screenId, fader) -> {
            if (currentProjectable.getValue() == null) {
                if (bgVideo.isRender().get() && !fader.getScreen().isChromaScreen()) {
                    fader.fadeIn(bgVideo);
                } else {
                    fader.fadeIn(background);
                }
            } else {
                fader.fadeIn(currentProjectable.get());
            }
        });
    }

    public void init() {
        if (initialized) {
            initializeList.forEach(Projectable::rebuildLayout);
        } else {
            initializeList.forEach(Projectable::init);
            initialized = true;
        }

        faders.clear();

        delegate.getVirtualScreens().forEach(vs -> {
            faders.put(vs.getVirtualScreenId(), new PaintableCrossFader(vs));
        });

        faders.forEach((screen, fader) -> {
            fader.fadeIn(background);
        });
    }

    public void finish() {
        initializeList.forEach(Projectable::finish);
    }

    protected void paintComponent(Graphics2D g, VirtualScreen vs) {
        PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

        if (fader != null) {
            fader.paintComponent(g);
        }

        label.paintComponent(g, vs);
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
    public BackgroundModel getBackgroundModel() {
        return (BackgroundModel) background.getModel();
    }

    @Override
    public ReadOnlyProperty<Projectable> projectableProperty() {
        return currentProjectable.getReadOnlyProperty();
    }

    @Override
    public void setBackgroundModel(BackgroundModel backgroundModel) {
        background.setModel(backgroundModel);
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        delegate.setFullScreen(fullScreen);
    }

    @Override
    public void setCropBackground(boolean selected) {
        background.setCropBackground(selected);
        ProjectorPreferences.setCropBackground(selected);
    }

    @Override
    public ProjectionWebView createWebView() {
        ProjectionWebView wv = new ProjectionWebView(delegate);
        initializeList.add(wv);
        wv.init();
        return wv;
    }

    @Override
    public void setProjectable(Projectable projectable) {
        this.currentProjectable.setValue(projectable);
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

    @Override
    public void setAnimateBackground(boolean selected) {
        background.setEnableAnimation(selected);
    }

    @Override
    public boolean getDarkenBackground() {
        return label.isDarkenBackground();
    }

    @Override
    public void setDarkenBackground(boolean darkenBg) {
        label.setDarkenBackground(darkenBg);
    }

    @Override
    public void stop(Projectable projectable) {
        projectable.finish();
        initializeList.remove(projectable);
    }

    @Override
    public void setMusicForBackground(Integer musicId, File preferred) {
        if (musicId != null) {
            bgVideo.startBackground(musicId, preferred);
        } else {
            bgVideo.stopBackground();
        }
    }

    @Override
    public boolean getCropBackground() {
        return background.getCropBackground();
    }
}
