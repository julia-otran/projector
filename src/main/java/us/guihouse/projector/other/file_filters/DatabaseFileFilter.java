/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other.file_filters;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author guilherme
 */
public class DatabaseFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String name = f.getName();

        if (name == null) {
            return false;
        }

        return name.endsWith(".db");
    }

    @Override
    public String getDescription() {
        return "SQLite (*.db)";
    }
}
