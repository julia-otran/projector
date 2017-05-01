/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.dtos;

import java.util.List;

/**
 *
 * @author guilherme
 */
public class ImportingMusicDTO {

    private String name;
    private String artist;
    private List<String> phrases;

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public List<String> getPhrases() {
        return phrases;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setPhrases(List<String> phrases) {
        this.phrases = phrases;
    }

}
