/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.repositories;

import br.com.projector.models.Music;
import br.com.projector.projection.text.WrappedText;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.table.TableModel;

/**
 *
 * @author guilherme
 */
public class MusicRepository {

    private final List<Music> musics = new ArrayList<>();
    private final DefaultListModel<String> musicsListModel;
    private final List<PhrasesRepository> phrasesRepos;
    private final PhrasesGrouper grouper;

    public MusicRepository() {
        this.musicsListModel = new DefaultListModel<>();
        this.phrasesRepos = new ArrayList<>();
        this.grouper = new PhrasesGrouper();
    }

    public PhrasesGrouper getGrouper() {
        return grouper;
    }

    public void regroupPhrases() {
        phrasesRepos.stream().forEach(r -> r.regroup());
    }

    public void add(Music m) {
        musics.add(m);
        PhrasesRepository repo = new PhrasesRepository(grouper, m);
        repo.regroup();
        phrasesRepos.add(repo);
        musicsListModel.addElement(m.getName());
    }

    public PhrasesRepository getPhrasesRepository(int index) {
        if (index < 0 || index >= phrasesRepos.size()) {
            return null;
        }

        return phrasesRepos.get(index);
    }

    public ListModel<String> getMusicsModel() {
        return musicsListModel;
    }

    public void clear() {
        musics.clear();
        musicsListModel.clear();
        phrasesRepos.clear();
    }

    public List<File> openFiles() {
        return musics.stream().map(Music::getFile).collect(Collectors.toList());
    }

    public TableModel getPhrasesModel(int selectedMusic) {
        return phrasesRepos.get(selectedMusic).getGroupedPhrasesModel();
    }

    public WrappedText getTextFor(int music, int selected) {
        if (music < 0 || selected < 0) {
            return WrappedText.blankText();
        }

        return phrasesRepos.get(music).getTextAt(selected);
    }
}
