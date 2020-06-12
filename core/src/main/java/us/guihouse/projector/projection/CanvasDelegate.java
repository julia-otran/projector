/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.GraphicsDevice;
import java.util.List;

import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.services.SettingsService;
import us.guihouse.projector.utils.WindowConfigsLoader;

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
