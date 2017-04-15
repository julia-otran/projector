/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.other;

import br.com.projector.projection.text.WrappedText;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author guilherme
 */
public class WrappedTextCellRenderer extends JList implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JList<String> list = this;

        if (value instanceof WrappedText) {
            WrappedText text = (WrappedText) value;
            list.setListData(text.getLines().toArray(new String[text.getLines().size()]));
        }

        //cell backgroud color when selected
        if (isSelected) {
            list.setBackground(table.getSelectionBackground());
            list.setForeground(table.getSelectionForeground());
        } else if (row % 2 == 0) {
            list.setBackground(table.getBackground());
            list.setForeground(table.getForeground());
        } else {
            list.setBackground(table.getGridColor());
            list.setForeground(table.getForeground());
        }

        list.setDragEnabled(false);
        list.clearSelection();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int width = table.getColumnModel().getColumn(column).getWidth();
        list.setSize(width, Short.MAX_VALUE);

        return list;
    }

}
