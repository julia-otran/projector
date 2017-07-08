/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.services;

import java.io.File;
import us.guihouse.projector.other.ProjectorPreferences;

/**
 *
 * @author guilherme
 */
public class SettingsService {

    public void storeLastBackground(File file) {
        if (file == null) {
            ProjectorPreferences.setBackgroundFilePath(null);
            return;
        }

        ProjectorPreferences.setBackgroundFilePath(file.getAbsolutePath());
    }

    public File getLastBackgroundImageFile() {
        String path = ProjectorPreferences.getBackgroundFilePath();

        if (path == null) {
            return null;
        }

        return new File(path);
    }

}
