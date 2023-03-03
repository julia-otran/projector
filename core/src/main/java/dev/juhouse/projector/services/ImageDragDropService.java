/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import dev.juhouse.projector.forms.controllers.projection.ImageController;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ImageDragDropService {
    public interface Client {
        void onFileLoading();
        void onFileOk();
        void onFileError(String message);
        
        void onDropSuccess(Image image, File imageFile);
        void onDropAbort();
        
        void showPreviewImage(Image image);
    }
    
    private final boolean requireFile;
    private final Client client;
    private File imageFile;
    private Image dropping;

    public ImageDragDropService(Client client, boolean requireFile) {
        this.client = client;
        this.requireFile = requireFile;
    }
    
    public void onDragOver(DragEvent event) {
        Dragboard board = event.getDragboard();
        
        Image input = null;
        
        if (board.hasFiles()) {
            if (board.getFiles().size() > 1) {
                client.onFileError("Não é possivel ler mais de um arquivo.");
                return;
            } else {
                File file = board.getFiles().get(0);
                InputStream is = null;
                
                try {
                    is = new FileInputStream(file);
                    input = new Image(is);
                    imageFile = file;
                } catch (Exception ex) {
                    Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
                    input = null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ex) {
                            Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        
        if (input == null && !requireFile) {
            try {
                imageFile = null;
                input = board.getImage();
            } catch (Exception ex) {
                Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (input == null) {
            client.onFileError("Imagem/Arquivo inválido");
            return;
        }
        
        event.acceptTransferModes(TransferMode.LINK);
        dropping = input;
        
        client.showPreviewImage(input);
        loadImage();
    }
    
    @FXML
    public void onDragExit() {
        dropping = null;
        client.showPreviewImage(null);
        client.onDropAbort();
    }
    
    @FXML
    public void onDragDropped(DragEvent event) {
        client.onDropSuccess(dropping, imageFile);
    }
    
    private void loadImage() {
        if (dropping.isBackgroundLoading()) {
            client.onFileLoading();
            
            dropping.errorProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    client.onFileError("Imagem ou arquivo inválido.");
                    client.showPreviewImage(null);
                    dropping = null;
                } else {
                    client.onFileOk();
                }
            });
        } else {
            if (dropping.isError()) {
                client.onFileError("Imagem ou arquivo inválido.");
                client.showPreviewImage(null);
                dropping = null;
            } else {
                client.onFileOk();
            }
        }
    }
}
