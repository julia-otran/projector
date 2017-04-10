/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection;

import java.awt.Font;

/**
 *
 * @author 15096134
 */
public interface ProjectionWindow {
    void setText(String string);
    void setFont(Font font);
    Font getFont();
}
