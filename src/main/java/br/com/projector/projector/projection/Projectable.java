/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.projection;

import java.awt.Graphics;

/**
 *
 * @author guilherme
 */
public interface Projectable {

    public void paintComponent(Graphics g);

    public CanvasDelegate getCanvasDelegate();

    public void rebuildLayout();

    public void init(ProjectionCanvas sourceCanvas);
}
