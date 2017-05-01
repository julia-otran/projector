/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.repositories;

import br.com.projector.projector.models.Music;
import java.io.File;
import java.nio.file.Files;
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
        try {
            Music m = new Music();
            m.setName(file.getName().replace(".txt", ""));

            List<String> lines = Files.readAllLines(file.toPath());
            m.setPhrases(lines);

            return m;
        } catch (Exception ex) {
            Logger.getLogger(MusicLoader.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static void loadFileToRepository(File source, OpenMusicRepository destination) {
        Music loaded = loadFromFile(source);

        if (loaded == null) {
            JOptionPane.showMessageDialog(null, source.getAbsolutePath(), "Erro ao carregar arquivo", JOptionPane.ERROR_MESSAGE);
        } else {
            destination.add(loaded);
        }
    }

    public static void loadFilesToRepository(File sources[], OpenMusicRepository destination) {
        for (File f : sources) {
            loadFileToRepository(f, destination);
        }
    }

    public static void loadFilesToRepository(List<File> sources, OpenMusicRepository destination) {
        sources.stream().forEach(f -> loadFileToRepository(f, destination));
    }
}
