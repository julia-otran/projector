/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection;

import java.awt.GraphicsDevice;
import java.util.List;

import dev.juhouse.projector.projection.models.VirtualScreen;
import dev.juhouse.projector.services.SettingsService;

/**
 *
 * @author guilherme
 */
public interface CanvasDelegate {

    int getMainWidth();

    int getMainHeight();

    List<VirtualScreen> getVirtualScreens();

    void setFullScreen(boolean fullScreen);

    SettingsService getSettingsService();

    GraphicsDevice getDefaultDevice();
}
