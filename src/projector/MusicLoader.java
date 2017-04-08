/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author guilherme
 */
public class MusicLoader {
    public static Music loadFromFile(File file) {
        Music m = new Music();
        m.setName(file.getName().replace(".txt", ""));
        
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            m.setPhrases(lines);
        } catch (IOException ex) {
            Logger.getLogger(MusicLoader.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        return m;
    }
    
    public static void loadFilesToRepository(File sources[], MusicRepository destination) {
        for (File f : sources) {
            Music loaded = loadFromFile(f);
            if (loaded == null) {
                JOptionPane.showMessageDialog(null, f.getAbsolutePath(), "Erro ao carregar arquivo", JOptionPane.ERROR_MESSAGE);
            } else {
                destination.add(loaded);
            }
        }
    }
}
