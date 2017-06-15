/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.models;

import java.util.List;

/**
 *
 * @author guilherme
 */
public class Music {

    private List<String> phrases;
    private int id;
    private String name;
    private Artist artist;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getPhrases() {
        return phrases;
    }

    public void setPhrases(List<String> phrases) {
        this.phrases = phrases;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public String getNameWithArtist() {
        if (artist == null) {
            return name;
        }

        return name + " - " + artist.getName();
    }

}
