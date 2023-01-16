/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.scenes;

import dev.juhouse.projector.models.ProjectionListItem;

/**
 *
 * @author guilherme
 */
public interface SceneObserver {

    void titleChanged(ProjectionListItem item);
}
