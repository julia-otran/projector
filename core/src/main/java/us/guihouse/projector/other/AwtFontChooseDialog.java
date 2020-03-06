/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.swing.JLabel;

/**
 *
 * @author guilherme
 */
public class AwtFontChooseDialog extends Dialog implements ChangeListener {

    public interface OnFontSelected {
        public void onFontChosed(Font font);
    }
    
    private Font font;
    
    private final JLabel previewLabel;
    private final ListView<String> fontnames;
    private final ListView<String> styles;
    private final ListView<String> sizes;
    
    public AwtFontChooseDialog(Font current, OnFontSelected callback) {
        this.font = current;
        
        VBox vbox = new VBox();
        HBox hbox = new HBox();
        
        hbox.setSpacing(10.0);
        
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        
        this.getDialogPane().getButtonTypes().add(cancel);
        this.getDialogPane().getButtonTypes().add(confirm);
        
        this.getDialogPane().setHeaderText("Alterar fonte");
        this.getDialogPane().setContent(vbox);
        this.setWidth(500.0);
        
        final Button btCancel = (Button) this.getDialogPane().lookupButton(cancel);
        final Button btConfirm = (Button) this.getDialogPane().lookupButton(confirm);
        
        btCancel.addEventFilter(ActionEvent.ACTION, event -> {
            AwtFontChooseDialog.this.close();
        });
        
        btConfirm.addEventFilter(ActionEvent.ACTION, event -> {
            callback.onFontChosed(font);
            AwtFontChooseDialog.this.close();
        });
        
        fontnames = new ListView<>();
        styles = new ListView<>();
        sizes = new ListView<>();
        
        previewLabel = new JLabel();
        previewLabel.setFont(font);
        previewLabel.setText("AaBbÇç");
        previewLabel.setMaximumSize(new Dimension(350, 200));
        
        hbox.getChildren().add(fontnames);
        hbox.getChildren().add(styles);
        hbox.getChildren().add(sizes);
        
        vbox.getChildren().add(hbox);
        
        SwingNode previewNode = new SwingNode();
        previewNode.setContent(previewLabel);
        vbox.getChildren().add(previewNode);
        
        fontnames.getItems().addAll(getFontNames());
        fontnames.getSelectionModel().select(current.getFamily());
        fontnames.getSelectionModel().selectedIndexProperty().addListener(this);
        
        sizes.getItems().addAll(getFontSizes());
        sizes.getSelectionModel().select(Integer.toString(current.getSize()));
        sizes.getSelectionModel().selectedIndexProperty().addListener(this);
        
        styles.getItems().addAll(getStyles());
        styles.getSelectionModel().select(getStyleIndex(current));
        styles.getSelectionModel().selectedIndexProperty().addListener(this);
    }
    
    private static String[] getFontNames() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }
    
    private static String[] getFontSizes() {
        int maxSize = 2048;
        int step = 16;
        
        String sizes[] = new String[maxSize / step];
        
        int i = 0;
        for (int s=16; s <= maxSize; s += step) {
            sizes[i] = Integer.toString(s);
            i++;
        }
        
        return sizes;
    }
    
    private static String[] getStyles() {
        return new String[]{
            "Normal", "Negrito", "Itálico", "Negrito e Itálico"
        };
    }
    
    private static int getStyleIndex(Font f) {
        if (f.isBold() && f.isItalic()) {
            return 3;
        } else if (f.isBold()) {
            return 1;
        } else if (f.isItalic()) {
            return 2;
        }
        
        return 0;
    }
    
    private static int getStyleFromIndex(int i) {
        switch (i) {
            case 1: return Font.BOLD;
            case 2: return Font.ITALIC;
            case 3: return Font.BOLD + Font.ITALIC;
            default: return Font.PLAIN;
        }
    } 
    
    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        updateFont();
    }
    
    private void updateFont() {
        String familyName = fontnames.getSelectionModel().getSelectedItem();
        int size;
        
        try {
            size = Integer.parseInt(sizes.getSelectionModel().getSelectedItem());
        } catch (NumberFormatException ex) {
            size = 16;
        }
        
        int style = getStyleFromIndex(styles.getSelectionModel().getSelectedIndex());
        
        this.font = new Font(familyName, style, size);
        previewLabel.setFont(font);
    }
}
