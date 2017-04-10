package br.com.projector.repositories;

import br.com.projector.models.Music;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author guilherme
 */
public class PhrasesRepository {
    private PhrasesGrouper grouper;
    private final Music music;
    
    public PhrasesRepository(PhrasesGrouper grouper, Music music) {
        this.grouper = grouper;
        this.music = music;
    }
}
