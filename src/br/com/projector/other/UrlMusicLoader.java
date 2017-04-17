/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.other;

import br.com.projector.models.Music;
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
public class UrlMusicLoader {

    public static Music extractData(String data) {
        Music music = new Music();

        Document doc = Jsoup.parse(data);
        Elements title = doc.select(".cnt-head_title h1");
        Elements artist = doc.select(".cnt-head_title h2");
        Elements stanzasElm = doc.select(".cnt-letra article p");

        music.setName(title.text() + " - " + artist.text());
        List<String> phrases = new ArrayList<>();

        for (Element stanza : stanzasElm) {
            String phrasesStr = stanza.html()
                    .replace("\r\n", "")
                    .replace("\n", "")
                    .replaceAll("<\\s*br\\s*/?\\s*>", "\n");

            phrases.addAll(Arrays.asList(phrasesStr.split("\n")));
            phrases.add("");
        }

        music.setPhrases(phrases);
        return music;
    }
}
