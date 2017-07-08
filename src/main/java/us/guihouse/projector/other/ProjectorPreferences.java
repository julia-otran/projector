/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other;

import java.util.prefs.Preferences;

/**
 *
 * @author guilherme
 */
public class ProjectorPreferences {

    private static final String NODE_NAME = "/us/guihouse/projector";
    private static Preferences prefs;

    private static final String SQLITE_FILE = "SQLITE_FILE";
    private static final String BACKGROUND_FILE_PATH = "BACKGROUND_FILE_PATH";

    public static Preferences getPrefs() {
        if (prefs == null) {
            prefs = Preferences.userRoot().node(NODE_NAME);
        }

        return prefs;
    }

    public static String getSqlitePath() {
        return getPrefs().get(SQLITE_FILE, "");
    }

    public static void setSqlitePath(String path) {
        if (path == null) {
            getPrefs().remove(SQLITE_FILE);
        } else {
            getPrefs().put(SQLITE_FILE, path);
        }
    }

    public static String getBackgroundFilePath() {
        return getPrefs().get(BACKGROUND_FILE_PATH, null);
    }

    public static void setBackgroundFilePath(String path) {
        if (path == null) {
            getPrefs().remove(BACKGROUND_FILE_PATH);
        } else {
            getPrefs().put(BACKGROUND_FILE_PATH, path);
        }
    }
}
