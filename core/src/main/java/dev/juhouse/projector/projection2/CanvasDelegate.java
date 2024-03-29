/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2;

import java.awt.*;

import dev.juhouse.projector.services.SettingsService;
import javafx.beans.property.Property;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface CanvasDelegate {

    int getMainWidth();

    int getMainHeight();

    int getTextWidth();

    int getTextHeight();

    Property<Font> getFontProperty();

    SettingsService getSettingsService();

    Bridge getBridge();
}
