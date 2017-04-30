/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.other;

import java.util.prefs.Preferences;

/**
 *
 * @author guilherme
 */
public class ProjectorPreferences {

    private static final String NODE_NAME = "/br/com/projector/projector";
    private static final Preferences PREFS = Preferences.userRoot().node(NODE_NAME);

    private static final String SQLITE_FILE = "SQLITE_FILE";

    public static Preferences getPrefs() {
        return PREFS;
    }

    public static String getSqlitePath() {
        return getPrefs().get(SQLITE_FILE, "");
    }

    public static void setSqlitePath(String path) {
        getPrefs().put(SQLITE_FILE, path);
    }
}
