/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author guilherme
 */
public class YouTubeVideoResolve implements Callback<String> {

    public static class YTVideoSearchEvent extends Event {
        private final boolean error;
        private final String resolved;
        
        private YTVideoSearchEvent(boolean error, String resolved) {
            super(EventType.ROOT);
            this.error = error;
            this.resolved = resolved;
        }

        public String getResolved() {
            return resolved;
        }

        public boolean isError() {
            return error;
        }
    }
    
    public static void getVideoEmbedUrl(EventHandler<YTVideoSearchEvent> handler) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("Digitar URL");
        inputDialog.setHeaderText("Copie e cole o link do vídeo do YouTube");
        inputDialog.setContentText("Link:");
        
        Optional<String> newValue = inputDialog.showAndWait();
        
        if (newValue.isPresent()) {
            YouTubeVideoResolve resolver = new YouTubeVideoResolve(newValue.get());
            resolver.setHandler(handler);
            resolver.start();
        } else {
            handler.handle(new YTVideoSearchEvent(false, null));
        }
    }
    
    private final String url;
    private EventHandler<YTVideoSearchEvent> handler;
    private Future<HttpResponse<String>> currentRequest;
    private Dialog<Object> currentDialog;
    
    YouTubeVideoResolve(String url) {
       this.url = url;
    }

    private void setHandler(EventHandler<YTVideoSearchEvent> handler) {
        this.handler = handler;
    }
    
    public void start() {
        showProgressDialog();
        try {
            this.currentRequest = Unirest.get(url).asStringAsync(this);
        } catch (Exception ex) {
            Logger.getLogger(YouTubeVideoResolve.class.getName()).log(Level.SEVERE, null, ex);
            this.failed(null);
        }
    }
    
    private void resolve(String url) {
        handler.handle(new YTVideoSearchEvent(false, url));
        handler = null;
    }
    
    private void reject(boolean errored) {
        handler.handle(new YTVideoSearchEvent(errored, null));
        handler = null;
    }

    @Override
    public void completed(HttpResponse<String> hr) {
        String found = null;
        
        Document doc = Jsoup.parse(hr.getBody());
        Elements urlMeta = doc.select("meta[property=og:video:url]");
        
        for (Element el : urlMeta) {
            String content = el.attr("content");
            
            if (content != null && content.contains("embed")) {
                found = content;
                break;
            }
        }
        
        final String found2 = found;
        
        Platform.runLater(() -> {
            currentRequest = null;

            if (currentDialog != null) {
                currentDialog.close();
                currentDialog = null;
            }

            if (found2 != null) {
                resolve(found2);
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
}
