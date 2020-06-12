/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

/**
 *
 * @author guilherme
 */
public interface Projectable extends Paintable {

    void rebuildLayout();

    void init();

    void finish();
}
