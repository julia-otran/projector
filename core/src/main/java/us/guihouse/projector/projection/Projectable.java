/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *
 * @author guilherme
 */
public interface Projectable extends Paintable {

    CanvasDelegate getCanvasDelegate();

    void rebuildLayout();

    void init();

    void finish();
}
