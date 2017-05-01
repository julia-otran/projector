/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.music_importing;

import br.com.projector.projector.dtos.ImportingMusicDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author guilherme
 */
public class VagalumeImporter extends MusicUrlImporter {

    public VagalumeImporter(String url) {
        super(url);
    }

    @Override
    protected ImportingMusicDTO parseMusic(String data) throws ImportError {
        ImportingMusicDTO music = new ImportingMusicDTO();

        Document doc = Jsoup.parse(data);
        Elements title = doc.select("#header h1");
        Elements artist = doc.select("#header p a");
        Elements stanzasElm = doc.select(".originalOnly div");

        music.setName(title.text());
        music.setArtist(artist.text());
        List<String> phrases = new ArrayList<>();

        for (Element stanza : stanzasElm) {
            String phrasesStr = stanza.html()
                    .replace("\r\n", "")
                    .replace("\n", "")
                    .replaceAll("<\\s*br\\s*/?\\s*>", "\n");

            phrases.addAll(Arrays.asList(phrasesStr.split("\n")));
            phrases.add("");
        }

        if (phrases.isEmpty()) {
            throw new ImportError("Cannot read phrases");
        }

        music.setPhrases(phrases);
        return music;
    }

}
