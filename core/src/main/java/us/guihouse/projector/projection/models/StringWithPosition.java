/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection.models;

/**
 *
 * @author guilherme
 */
public class StringWithPosition {

    private final int x;
    private final int y;
    private final String text;

    public StringWithPosition(String text, int x, int y) {
        this.text = text;
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getText() {
        return text;
    }
}
