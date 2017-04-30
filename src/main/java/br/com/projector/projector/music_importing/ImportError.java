/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.music_importing;

/**
 *
 * @author guilherme
 */
class ImportError extends Exception {

    ImportError(String message) {
        super(message);
    }

    ImportError(Exception source) {
        super(source);
    }
}
