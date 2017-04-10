/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection.models;

/**
 *
 * @author guilherme
 */
public class StringWithWidth {
    private final String string;
    private final int width;
        
    public StringWithWidth(String string, int width) {
        this.string = string;
        this.width = width;
    }
    
    public String getString() {
        return string;
    }
    
    public int getWidth() {
        return width;
    }
}
