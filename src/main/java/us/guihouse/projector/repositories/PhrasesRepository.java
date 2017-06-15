package us.guihouse.projector.repositories;

import us.guihouse.projector.models.Music;
import us.guihouse.projector.projection.text.WrappedText;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.TableModel;

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
    private final PhrasesTableModel groupedPhrasesModel;

    PhrasesRepository(PhrasesGrouper grouper, Music music) {
        this.grouper = grouper;
        this.music = music;
        this.groups = new ArrayList<>();
        this.groupedPhrasesModel = new PhrasesTableModel();
    }

    public TableModel getGroupedPhrasesModel() {
        return groupedPhrasesModel;
    }

    public WrappedText getTextAt(int position) {
        if (position < 0 || position >= groups.size()) {
            return WrappedText.blankText();
        }

        return groups.get(position);
    }

    public void regroup() {
        List<WrappedText> groups = grouper.groupMusic(music);
        this.groups.clear();
        this.groups.addAll(groups);
        this.groupedPhrasesModel.setGroups(groups);
    }
}
