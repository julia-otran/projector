/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.other;

import com.sun.istack.internal.NotNull;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

/**
 *
 * @author guilherme
 */
public class GeneralKeyboardDispatcher implements KeyEventDispatcher {

    public interface Listener {

        void onKeyboardEscPressed();
    }

    private final Listener listener;

    public GeneralKeyboardDispatcher(@NotNull Listener listener) {
        this.listener = listener;
    }

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
