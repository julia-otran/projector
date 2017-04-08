/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author guilherme
 */
public class MusicRepository {
    private List<Music> musics = new ArrayList<>();
    private DefaultListModel<String> musicsListModel = new DefaultListModel<>();
    
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
                return selected.getPhrases().get(index);
            }

            @Override
            public void addListDataListener(ListDataListener l) { }

            @Override
            public void removeListDataListener(ListDataListener l) { }
        };
    }
}
