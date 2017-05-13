/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.repositories;

import br.com.projector.projector.models.Music;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author guilherme
 */
public class MusicTableModel extends DefaultTableModel {

    private static final String COLUMN_NAMES[] = new String[]{"Música", "Artista", "Letra"};

    public MusicTableModel() {
        super();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void setMusics(List<Music> group) {
        Object data[][] = new Object[group.size()][COLUMN_NAMES.length];

        for (int i = 0; i < data.length; i++) {
            Music m = group.get(i);
            data[i] = new Object[]{
                m.getName(),
                m.getArtist().getName(),
                m.getPhrases().stream().collect(Collectors.joining("\n"))
            };
        }

        setDataVector(data, COLUMN_NAMES);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return super.getColumnClass(columnIndex);
    }
}
