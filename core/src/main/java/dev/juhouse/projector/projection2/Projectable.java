/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import javafx.beans.property.ReadOnlyObjectProperty;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface Projectable {

    void init();

    void finish();

    void rebuild();

    void setRender(boolean render);

    BridgeRenderFlag getRenderFlag();
}
