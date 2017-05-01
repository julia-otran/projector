/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.music_importing;

import br.com.projector.projector.dtos.ImportingMusicDTO;
import br.com.projector.projector.other.ProgressDialog.Executor;
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

    public Executor getExecutor(final ImportCallback callback) {
        return new Executor() {
            ImportingMusicDTO parsed;
            boolean success;

            @Override
            public void doInBackground() {
                try {
                    String data = doRequest();
                    parsed = parseMusic(data);
                    success = true;
                } catch (ImportError ex) {
                    Logger.getLogger(MusicUrlImporter.class.getName()).log(Level.SEVERE, null, ex);
                    success = false;
                }
            }

            @Override
            public void done() {
                if (success) {
                    callback.onImportSuccess(parsed);
                } else {
                    callback.onImportError();
                }
            }

        };
    }

    public String getUrl() {
        return url;
    }

    protected abstract ImportingMusicDTO parseMusic(String data) throws ImportError;

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
}
