/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.music_importing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.guihouse.projector.dtos.ImportingMusicDTO;

/**
 *
 * @author guilherme
 */
public class LetrasMusImporter extends MusicUrlImporter {

    LetrasMusImporter(String url) {
        super(url);
    }

    @Override
    protected ImportingMusicDTO parseMusic(String data) throws ImportError {
        ImportingMusicDTO music = new ImportingMusicDTO();

        Document doc = Jsoup.parse(data);
        Elements title = doc.select(".cnt-head_title h1");
        Elements artist = doc.select(".cnt-head_title h2");
        Elements stanzasElm = doc.select(".cnt-letra p");

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
