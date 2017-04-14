/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection;

import br.com.projector.projection.text.WrappedText;
import java.awt.Font;

/**
 *
 * @author 15096134
 */
public interface ProjectionManager {
    void setText(WrappedText text);
    
    Font getTextFont();
    void setTextFont(Font font);
    
    TextWrapperFactoryChangeListener getTextWrapperChangeListener();
    void setTextWrapperChangeListener(TextWrapperFactoryChangeListener wrapperChangeListener);
    
}
