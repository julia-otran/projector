/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.other;

import dev.juhouse.projector.utils.FontCreatorUtil;

import java.awt.*;
import java.util.prefs.Preferences;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ProjectorPreferences {

    private static final String NODE_NAME = "/dev/juhouse/projector";
    private static Preferences prefs;

    private static final String SQLITE_FILE = "SQLITE_FILE";
    private static final String BACKGROUND_STATIC_FILE_PATH = "BACKGROUND_FILE_PATH";
    private static final String BACKGROUND_CROP = "BACKGROUND_CROP";
    private static final String WINDOW_CONFIG_FILE = "WINDOW_CONFIG_FILE";
    private static final String PROJECTION_LABEL_FONT_SIZE = "PROJECTION_LABEL_FONT_SIZE";
    private static final String PROJECTION_LABEL_FONT_NAME = "PROJECTION_LABEL_FONT_NAME";
    private static final String PROJECTION_LABEL_FONT_STYLE = "PROJECTION_LABEL_FONT_STYLE";

    public static Preferences getPrefs() {
        if (prefs == null) {
            prefs = Preferences.userRoot().node(NODE_NAME);
        }

        return prefs;
    }

    public static String getSqlitePath() {
        return getPrefs().get(SQLITE_FILE, "");
    }

    public static String getWindowConfigFile() {
        return getPrefs().get(WINDOW_CONFIG_FILE, null);
    }

    public static void setWindowConfigFile(String fileName) {
        if (fileName == null) {
            getPrefs().remove(WINDOW_CONFIG_FILE);
        } else {
            getPrefs().put(WINDOW_CONFIG_FILE, fileName);
        }
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

    public static boolean getCropBackground() { return getPrefs().getBoolean(BACKGROUND_CROP, true); }

    public static void setCropBackground(boolean crop) { getPrefs().putBoolean(BACKGROUND_CROP, crop); }

    public static int getProjectionLabelFontSize() {
        return getPrefs().getInt(PROJECTION_LABEL_FONT_SIZE, 112);
    }

    public static void setProjectionLabelFontSize(int size) {
        getPrefs().putInt(PROJECTION_LABEL_FONT_SIZE, size);
    }

    public static String getProjectionLabelFontName() {
        return getPrefs().get(PROJECTION_LABEL_FONT_NAME, FontCreatorUtil.getMontserratFont().getFamily());
    }

    public static void setProjectionLabelFontName(String name) {
        getPrefs().put(PROJECTION_LABEL_FONT_NAME, name);
    }

    public static int getProjectionLabelFontStyle() {
        return getPrefs().getInt(PROJECTION_LABEL_FONT_STYLE, Font.PLAIN);
    }

    public static void setProjectionLabelFontStyle(int style) {
        getPrefs().putInt(PROJECTION_LABEL_FONT_STYLE, style);
    }
}
