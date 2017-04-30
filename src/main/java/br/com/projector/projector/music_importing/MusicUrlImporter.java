/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.music_importing;

import br.com.projector.projector.models.Music;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author guilherme
 */
public abstract class MusicUrlImporter {

    private final String url;

    protected MusicUrlImporter(String url) {
        this.url = url;
    }

    public Runnable getExecutor(final ImportCallback callback) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    String data = doRequest();
                    final Music parsed = parseMusic(data);
                    dispatchSuccess(parsed, callback);
                } catch (ImportError ex) {
                    Logger.getLogger(MusicUrlImporter.class.getName()).log(Level.SEVERE, null, ex);
                    dispatchError(callback);
                }
            }

        };
    }

    public String getUrl() {
        return url;
    }

    protected abstract Music parseMusic(String data) throws ImportError;

    private String doRequest() throws ImportError {
        try {
            return Unirest.get(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .asString()
                    .getBody();
        } catch (UnirestException ex) {
            Logger.getLogger(MusicUrlImporter.class.getName()).log(Level.SEVERE, null, ex);
            throw new ImportError(ex);
        }
    }

    private void dispatchSuccess(final Music music, final ImportCallback callback) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                callback.onImportSuccess(music);
            }
        });
    }

    private void dispatchError(final ImportCallback callback) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                callback.onImportError();
            }
        });
    }
}
