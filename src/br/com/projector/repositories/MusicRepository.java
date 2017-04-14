/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.repositories;

import br.com.projector.models.Music;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author guilherme
 */
public class MusicRepository {
    private final List<Music> musics = new ArrayList<>();
    private final DefaultListModel<String> musicsListModel;

    public MusicRepository() {
        this.musicsListModel = new DefaultListModel<>();
    }
    
    public void add(Music m) {
        musics.add(m);
        musicsListModel.addElement(m.getName());
    }
    
    public ListModel<String> getMusicsModel() {
        return musicsListModel;
    }

    public void clear() {
        musics.clear();
        musicsListModel.clear();
    }

    public List<File> openFiles() {
        return musics.stream().map(Music::getFile).collect(Collectors.toList());
    }
}
