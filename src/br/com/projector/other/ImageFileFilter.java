/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.other;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author guilherme
 */
public class ImageFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String name = f.getName();

        if (name == null) {
            return false;
        }

        return name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg");
    }

    @Override
    public String getDescription() {
        return "Imagens (*.jpg, *.png)";
    }
}
