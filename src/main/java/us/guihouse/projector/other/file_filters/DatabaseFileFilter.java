/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other.file_filters;

import java.io.File;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 *
 * @author guilherme
 */
public class DatabaseFileFilter {
    public static ExtensionFilter getFilter() {
        return new ExtensionFilter("Arquivos do SQLite (*.db)", "*.db");
    }
}
