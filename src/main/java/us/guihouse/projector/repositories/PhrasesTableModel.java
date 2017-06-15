/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.repositories;

import us.guihouse.projector.projection.text.WrappedText;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author guilherme
 */
public class PhrasesTableModel extends DefaultTableModel {

    private static final String COLUMN_NAMES[] = new String[]{"Letra"};

    public PhrasesTableModel() {
        super();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    void setGroups(List<WrappedText> groups) {
        Object data[][] = new Object[groups.size()][0];

        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[]{groups.get(i)};
        }

        setDataVector(data, COLUMN_NAMES);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return WrappedText.class;
        }

        return super.getColumnClass(columnIndex);
    }
}
