/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author guilherme
 */
public class TextFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        
        String name = f.getName();
        
        if (name == null) {
            return false;
        }
        
        if (name.endsWith(".txt")) {
            return true;
        }
        
        return false;
    }

    @Override
    public String getDescription() {
        return "Arquivos texto (*.txt)";
    }
    
}
