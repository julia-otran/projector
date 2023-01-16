/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.other;

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
public class AwtFontChooseDialog extends Dialog<Object> implements ChangeListener<Number> {

    public interface OnFontSelected {
        void onFontChosen(Font font);
    }
    
    private Font font;
    
    private final JLabel previewLabel;
    private final ListView<String> fontNames;
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
        
        btCancel.addEventFilter(ActionEvent.ACTION, event -> AwtFontChooseDialog.this.close());
        
        btConfirm.addEventFilter(ActionEvent.ACTION, event -> {
            callback.onFontChosen(font);
            AwtFontChooseDialog.this.close();
        });
        
        fontNames = new ListView<>();
        styles = new ListView<>();
        sizes = new ListView<>();
        
        previewLabel = new JLabel();
        updatePreviewFont(font);
        previewLabel.setText("AaBbÇç");
        previewLabel.setMaximumSize(new Dimension(350, 200));
        
        hbox.getChildren().add(fontNames);
        hbox.getChildren().add(styles);
        hbox.getChildren().add(sizes);
        
        vbox.getChildren().add(hbox);
        
        SwingNode previewNode = new SwingNode();
        previewNode.setContent(previewLabel);
        vbox.getChildren().add(previewNode);
        
        fontNames.getItems().addAll(getFontNames());
        fontNames.getSelectionModel().select(current.getFamily());
        fontNames.getSelectionModel().selectedIndexProperty().addListener(this);
        
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
        
        String[] sizes = new String[maxSize / step];
        
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
    
    @Override
    public void changed(ObservableValue observable, Number oldValue, Number newValue) {
        updateFont();
    }

    private void updatePreviewFont(Font font) {
        previewLabel.setFont(new Font(font.getFontName(), font.getStyle(), 32));
    }

    private void updateFont() {
        String familyName = fontNames.getSelectionModel().getSelectedItem();
        int size;
        
        try {
            size = Integer.parseInt(sizes.getSelectionModel().getSelectedItem());
        } catch (NumberFormatException ex) {
            size = 16;
        }
        
        int style;

        switch (styles.getSelectionModel().getSelectedIndex()) {
            case 1: style = Font.BOLD; break;
            case 2: style = Font.ITALIC; break;
            case 3: style = Font.BOLD + Font.ITALIC; break;
            default: style = Font.PLAIN;
        }

        this.font = new Font(familyName, style, size);
        updatePreviewFont(font);
    }
}
