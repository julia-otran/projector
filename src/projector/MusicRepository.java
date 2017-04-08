/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

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
    
    ListModel<String> getMusicsModel() {
        return musicsListModel;
    }

    ListModel<String> getPhrasesModel(int selectedMusic) {
        final Music selected = musics.get(selectedMusic);
        
        return new ListModel<String>() {
            @Override
            public int getSize() {
                return selected.getPhrases().size();
            }

            @Override
            public String getElementAt(int index) {
                String phrase = selected.getPhrases().get(index);
                
                if (phrase.isEmpty()) {
                    return " ";
                }
                
                return phrase;
            }

            @Override
            public void addListDataListener(ListDataListener l) { }

            @Override
            public void removeListDataListener(ListDataListener l) { }
        };
    }

    String getPhrasesUnion(int music, int[] selected) {
        Music m = musics.get(music);
        
        StringBuilder phrases = new StringBuilder();
        for (int l : selected) {
            phrases.append(m.getPhrases().get(l)).append("\n");
        }
        
        return phrases.toString();
    }

    void clear() {
        musics.clear();
        musicsListModel.clear();
    }

    List<File> openFiles() {
        return musics.stream().map(Music::getFile).collect(Collectors.toList());
    }
}
