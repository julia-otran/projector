/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.services;

import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class FileDragDropService {
    public interface Client {
        void onFileOk();
        void onFileError(String message);
        
        void onDropSuccess(File file);
        void onDropAbort();
    }
    
    private final Client client;
    private File dropped;
    
    public FileDragDropService(Client client) {
        this.client = client;
    }
    
    public void onDragOver(DragEvent event) {
        Dragboard board = event.getDragboard();
        this.dropped = null;
        
        if (board.hasFiles()) {
            if (board.getFiles().size() > 1) {
                client.onFileError("Não é possivel ler mais de um arquivo.");
                return;
            } else {
                File file = board.getFiles().get(0);
                
                if (file.exists() && file.canRead()) {
                    this.dropped = file;
                }
            }
        }
        
        if (this.dropped == null) {
            client.onFileError("Arquivo ilegível");
            return;
        }
        
        event.acceptTransferModes(TransferMode.LINK);

        client.onFileOk();
    }
    
    @FXML
    public void onDragExit() {
        this.dropped = null;
        client.onDropAbort();
    }
    
    @FXML
    public void onDragDropped(DragEvent event) {
        client.onDropSuccess(this.dropped);
    }
}
