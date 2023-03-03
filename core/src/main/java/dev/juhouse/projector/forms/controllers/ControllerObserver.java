/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface ControllerObserver {
    void onTitleChanged(String newTitle);
    void updateProperty(String key, String value);
    String getProperty(String key);
}
