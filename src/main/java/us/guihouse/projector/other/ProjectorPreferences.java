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
    private static final String BACKGROUND_STATIC_FILE_PATH = "BACKGROUND_FILE_PATH";
    private static final String BACKGROUND_FILE_PATH = "BACKGROUND2_FILE_PATH";
    private static final String BACKGROUND_LOGO_FILE_PATH = "BACKGROUND_LOGO_FILE_PATH";
    private static final String BACKGROUND_OVERLAY_FILE_PATH = "BACKGROUND_OVERLAY_FILE_PATH";
    private static final String BACKGROUND_TYPE = "BACKGROUND_TYPE";

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

    public static String getBackgroundStaticFilePath() {
        return getPrefs().get(BACKGROUND_STATIC_FILE_PATH, null);
    }

    public static void setBackgroundStaticFilePath(String path) {
        if (path == null) {
            getPrefs().remove(BACKGROUND_STATIC_FILE_PATH);
        } else {
            getPrefs().put(BACKGROUND_STATIC_FILE_PATH, path);
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

    public static String getBackgroundLogoFilePath() {
        return getPrefs().get(BACKGROUND_LOGO_FILE_PATH, null);
    }

    public static void setBackgroundLogoFilePath(String path) {
        if (path == null) {
            getPrefs().remove(BACKGROUND_LOGO_FILE_PATH);
        } else {
            getPrefs().put(BACKGROUND_LOGO_FILE_PATH, path);
        }
    }

    public static String getBackgroundOverlayFilePath() {
        return getPrefs().get(BACKGROUND_OVERLAY_FILE_PATH, null);
    }

    public static void setBackgroundOverlayFilePath(String path) {
        if (path == null) {
            getPrefs().remove(BACKGROUND_OVERLAY_FILE_PATH);
        } else {
            getPrefs().put(BACKGROUND_OVERLAY_FILE_PATH, path);
        }
    }

    public static String getBackgroundType() {
        return getPrefs().get(BACKGROUND_TYPE, null);
    }

    public static void setBackgroundType(String path) {
        if (path == null) {
            getPrefs().remove(BACKGROUND_TYPE);
        } else {
            getPrefs().put(BACKGROUND_TYPE, path);
        }
    }
}
