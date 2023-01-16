/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers;

/**
 *
 * @author guilherme
 */
public interface BackCallback {
    void goBack();
    void goBackWithId(Integer id);

    void goBackAndReload();
}
