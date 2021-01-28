/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import java.awt.image.BufferedImage;

import us.guihouse.projector.models.WindowConfig;
import us.guihouse.projector.other.GraphicsFinder;

/**
 *
 * @author guilherme
 */
public interface ProjectionWindow {
    void init(WindowConfig wc);
    void shutdown();
    void updateOutput(BufferedImage src);
    void updateWindowConfig(WindowConfig wc);
    void makeVisible();
    GraphicsFinder.Device getCurrentDevice();
}
