/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.projection;

/**
 *
 * @author guilherme
 */
public interface CanvasDelegate {

    public void repaint();

    public int getWidth();

    public int getHeight();
}
