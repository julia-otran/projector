package br.com.projector.repositories;

import br.com.projector.models.Music;
import br.com.projector.projection.text.WrappedText;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author guilherme
 */
public class PhrasesRepository {
    private final PhrasesGrouper grouper;
    private final Music music;
    private final List<WrappedText> groups;
    private final DefaultListModel<String> groupedPhrasesModel;
    
    PhrasesRepository(PhrasesGrouper grouper, Music music) {
        this.grouper = grouper;
        this.music = music;
        this.groups = new ArrayList<>();
        this.groupedPhrasesModel = new DefaultListModel<>();
    }
    
    public DefaultListModel<String> getGroupedPhrasesModel() {
        return groupedPhrasesModel;
    }
    
    public WrappedText getTextAt(int position) {
        if (position < 0 || position >= groups.size()) {
            return WrappedText.blankText();
        }
        
        return groups.get(position);
    }
    
    public void regroup() {
        groups.clear();
        groups.addAll(grouper.groupMusic(music));
        
        List<String> strings = groups.stream()
                .map(wt -> wt.getLines().stream().collect(Collectors.joining("\n")))
                .collect(Collectors.toList());
        
        groupedPhrasesModel.clear();
        
        for (String s : strings) {
            groupedPhrasesModel.addElement(s);
        }
    }
}
