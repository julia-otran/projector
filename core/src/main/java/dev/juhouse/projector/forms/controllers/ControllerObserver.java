/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

import java.util.Optional;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface ControllerObserver {
    void onTitleChanged(String newTitle);

    void beginPropertiesUpdate();

    void updateProperty(String key, String value);

    void finishPropertiesUpdate();

    Optional<String> getProperty(String key);
}
