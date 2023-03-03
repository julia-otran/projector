/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.other;

import lombok.Getter;
import lombok.Setter;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class GeneralKeyboardDispatcher implements KeyEventDispatcher {

    public interface Listener {

        void onKeyboardEscPressed();
    }

    @Getter
    @Setter
    private Listener listener;

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                listener.onKeyboardEscPressed();
            }
        }

        return false;
    }
}
