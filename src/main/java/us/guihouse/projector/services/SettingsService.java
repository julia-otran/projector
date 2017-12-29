/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.services;

import java.io.File;
import us.guihouse.projector.other.ProjectorPreferences;
import us.guihouse.projector.projection.models.BackgroundModel;

/**
 *
 * @author guilherme
 */
public class SettingsService {
    private static final String NONE_TYPE = "NONE";
    private static final String STATIC_TYPE = "STATIC";
    private static final String OVERLAY_ANIMATED = "OVERLAY_ANIMATED";

    public void storeLastBackground(BackgroundModel model) {
        if (model.getStaticBackground() == null) {
            ProjectorPreferences.setBackgroundStaticFilePath(null);
        } else {
            ProjectorPreferences.setBackgroundStaticFilePath(model.getStaticBackgroundFile().getAbsolutePath());
        }

        if (model.getBackgroundFile() == null) {
            ProjectorPreferences.setBackgroundFilePath(null);
        } else {
            ProjectorPreferences.setBackgroundFilePath(model.getBackgroundFile().getAbsolutePath());
        }

        if (model.getLogoFile() == null) {
            ProjectorPreferences.setBackgroundLogoFilePath(null);
        } else {
            ProjectorPreferences.setBackgroundLogoFilePath(model.getLogoFile().getAbsolutePath());
        }

        if (model.getOverlayFile() == null) {
            ProjectorPreferences.setBackgroundOverlayFilePath(null);
        } else {
            ProjectorPreferences.setBackgroundOverlayFilePath(model.getOverlayFile().getAbsolutePath());
        }

        if (BackgroundModel.Type.OVERLAY_ANIMATED.equals(model.getType())) {
            ProjectorPreferences.setBackgroundType(OVERLAY_ANIMATED);
        } else if (BackgroundModel.Type.STATIC.equals(model.getType())) {
            ProjectorPreferences.setBackgroundType(STATIC_TYPE);
        } else {
            ProjectorPreferences.setBackgroundType(NONE_TYPE);
        }
    }

    public BackgroundModel getLastBackground() {
        BackgroundModel model = new BackgroundModel();

        String typeStr = ProjectorPreferences.getBackgroundType();
        BackgroundModel.Type type = null;

        String path = ProjectorPreferences.getBackgroundStaticFilePath();

        if (typeStr == null && path == null) {
            type = BackgroundModel.Type.NONE;
        }

        if (typeStr == null && path != null) {
            type = BackgroundModel.Type.STATIC;
        }

        if (NONE_TYPE.equals(typeStr)) {
            type = BackgroundModel.Type.NONE;
        }

        if (STATIC_TYPE.equals(typeStr)) {
            type = BackgroundModel.Type.STATIC;
        }

        if (OVERLAY_ANIMATED.equals(typeStr)) {
            type = BackgroundModel.Type.OVERLAY_ANIMATED;
        }

        model.setType(type);

        if (path == null) {
            model.setStaticBackgroundFile(null);
        } else {
            model.setStaticBackgroundFile(new File(path));
        }

        if (ProjectorPreferences.getBackgroundFilePath() == null) {
            model.setBackgroundFile(null);
        } else {
            model.setBackgroundFile(new File(ProjectorPreferences.getBackgroundFilePath()));
        }

        if (ProjectorPreferences.getBackgroundLogoFilePath() == null) {
            model.setLogoFile(null);
        } else {
            model.setLogoFile(new File(ProjectorPreferences.getBackgroundLogoFilePath()));
        }

        if (ProjectorPreferences.getBackgroundOverlayFilePath() == null) {
            model.setOverlayFile(null);
        } else {
            model.setOverlayFile(new File(ProjectorPreferences.getBackgroundOverlayFilePath()));
        }

        return model;
    }

}
