/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.music_importing;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import dev.juhouse.projector.dtos.ImportingMusicDTO;

/**
 *
 * @author guilherme
 */
public abstract class MusicUrlImporter implements Callback<String> {

    private final String url;
    private Future<HttpResponse<String>> currentRequest;
    private Dialog<Object> currentDialog;
    private ImportCallback callback;

    protected MusicUrlImporter(String url) {
        this.url = url;
    }

    public void execute(final ImportCallback callback) {
        this.callback = callback;
        showProgressDialog();
        
        currentRequest = Unirest.get(url)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .asStringAsync(this);
    }

    protected abstract ImportingMusicDTO parseMusic(String data) throws ImportError;

    private void showProgressDialog() {
        ProgressBar pb = new ProgressBar();
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.prefWidth(200.0);
        
        VBox vbox = new VBox();
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.prefWidth(200.0);
        vbox.setPadding(new Insets(20.0));
        vbox.getChildren().add(pb);
        
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        Dialog<Object> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.getDialogPane().setHeaderText("Carregando...");
        dialog.getDialogPane().setContent(vbox);
        dialog.setWidth(500.0);
        
        final Button btCancel = (Button) dialog.getDialogPane().lookupButton(cancel);
        btCancel.addEventFilter(ActionEvent.ACTION, event -> {
            if (currentRequest != null) {
                currentRequest.cancel(true);
            }
        });
        
        currentDialog = dialog;
        dialog.show();
    }
    
    private void resolve(ImportingMusicDTO music) {
        callback.onImportSuccess(music);
    }
    
    private void reject(boolean errored) {
        callback.onImportError(errored);
    }
    
    @Override
    public void completed(HttpResponse<String> hr) {
        ImportingMusicDTO im = null;
        
        try {
            im = parseMusic(hr.getBody());
        } catch (ImportError ex) {
            Logger.getLogger(MusicUrlImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        final ImportingMusicDTO importing = im;
        
        Platform.runLater(() -> {
            currentRequest = null;

            if (currentDialog != null) {
                currentDialog.close();
                currentDialog = null;
            }

            if (importing != null) {
                resolve(importing);
            } else {
                reject(true);
            }
        });
    }

    @Override
    public void failed(UnirestException ue) {
        Platform.runLater(() -> {
            currentRequest = null;

            if (currentDialog != null) {
                currentDialog.close();
                currentDialog = null;
            }

            reject(true);
        });
    }

    @Override
    public void cancelled() {
        Platform.runLater(() -> {
            currentRequest = null;

            if (currentDialog != null) {
                currentDialog.close();
                currentDialog = null;
            }

            reject(false);
        });
    }
}
