/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.repositories;

import br.com.projector.projector.models.Music;
import br.com.projector.projector.projection.text.WrappedText;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.table.TableModel;

/**
 *
 * @author guilherme
 */
public class OpenMusicRepository {

    private final List<OpenMusic> musics = new ArrayList<>();
    private final DefaultListModel<String> musicsListModel;
    private final PhrasesGrouper grouper;
    private final MetricsRepository metricsRepo;

    private class OpenMusic {

        Music music;
        PhrasesRepository phrasesRepo;
        int position;
    }

    public OpenMusicRepository() {
        this.musicsListModel = new DefaultListModel<>();
        this.grouper = new PhrasesGrouper();
        this.metricsRepo = new MetricsRepository();
    }

    public PhrasesGrouper getGrouper() {
        return grouper;
    }

    public void regroupPhrases() {
        musics.stream().forEach(r -> r.phrasesRepo.regroup());
    }

    public void add(Music m) {
        try {
            metricsRepo.registerPlay(m);
        } catch (SQLException ex) {
            Logger.getLogger(OpenMusicRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PhrasesRepository repo = new PhrasesRepository(grouper, m);
        repo.regroup();

        OpenMusic opened = new OpenMusic();
        opened.music = m;
        opened.phrasesRepo = repo;
        opened.position = musics.size();

        musics.add(opened.position, opened);
        musicsListModel.add(opened.position, m.getNameWithArtist());
    }

    public PhrasesRepository getPhrasesRepository(int index) {
        if (index < 0 || index >= musics.size()) {
            return null;
        }

        return musics.get(index).phrasesRepo;
    }

    public ListModel<String> getMusicsModel() {
        return musicsListModel;
    }

    public void clear() {
        musics.clear();
        musicsListModel.clear();
    }

    public TableModel getPhrasesModel(int selectedMusic) {
        return musics.get(selectedMusic).phrasesRepo.getGroupedPhrasesModel();
    }

    public WrappedText getTextFor(int music, int selected) {
        if (music < 0 || selected < 0) {
            return WrappedText.blankText();
        }

        return musics.get(music).phrasesRepo.getTextAt(selected);
    }

    public boolean contains(Music m) {
        return musics.stream().anyMatch(om -> Objects.equals(om.music, m));
    }

    public void updateMusic(Music m) {
        Optional<OpenMusic> openedMusic = musics.stream()
                .filter(mo -> mo.music.getId() == m.getId())
                .findAny();

        if (!openedMusic.isPresent()) {
            return;
        }

        PhrasesRepository repo = new PhrasesRepository(grouper, m);
        repo.regroup();

        OpenMusic opened = openedMusic.get();
        opened.music = m;
        opened.phrasesRepo = repo;

        musicsListModel.remove(opened.position);
        musicsListModel.insertElementAt(m.getNameWithArtist(), opened.position);
    }
}
