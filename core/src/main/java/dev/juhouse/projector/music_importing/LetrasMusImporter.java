/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.music_importing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import dev.juhouse.projector.dtos.ImportingMusicDTO;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class LetrasMusImporter extends MusicUrlImporter {

    LetrasMusImporter(String url) {
        super(url);
    }

    @Override
    protected ImportingMusicDTO parseMusic(String data) throws ImportError {
        ImportingMusicDTO music = new ImportingMusicDTO();

        Document doc = Jsoup.parse(data);
        Elements title = doc.select(".head.--lyric .head-title");
        Elements artist = doc.select(".head.--lyric .head-subtitle ");
        Elements stanzasElm = doc.select(".lyric .lyric-original p");

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
            throw new ImportError();
        }

        music.setPhrases(phrases);
        return music;
    }

}
