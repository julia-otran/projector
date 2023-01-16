/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.services;

import java.io.File;

import dev.juhouse.projector.projection.models.BackgroundModel;
import dev.juhouse.projector.other.ProjectorPreferences;

/**
 *
 * @author guilherme
 */
public class SettingsService {
    private static final String NONE_TYPE = "NONE";
    private static final String STATIC_TYPE = "STATIC";

    public void storeLastBackground(BackgroundModel model) {
        if (model.getStaticBackground() == null) {
            ProjectorPreferences.setBackgroundStaticFilePath(null);
        } else {
            ProjectorPreferences.setBackgroundStaticFilePath(model.getStaticBackgroundFile().getAbsolutePath());
        }

    }

    public BackgroundModel getLastBackground() {
        BackgroundModel model = new BackgroundModel();

        String path = ProjectorPreferences.getBackgroundStaticFilePath();

        if (path == null) {
            model.setStaticBackgroundFile(null);
        } else {
            model.setStaticBackgroundFile(new File(path));
        }

        return model;
    }

}
