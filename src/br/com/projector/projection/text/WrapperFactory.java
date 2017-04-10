/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection.text;

import java.awt.FontMetrics;

/**
 *
 * @author guilherme
 */
public class WrapperFactory {
    private final int maxWidth; 
    private final int maxHeight;
            
    public WrapperFactory(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
    
    public TextWrapper getTextWrapper(FontMetrics fontMetrics, boolean multiPhrases) {
        if (multiPhrases) {
            return new NormalTextWrapper(fontMetrics, maxWidth, maxHeight);
        }
        
        return new CommaTextWrapper(fontMetrics, maxWidth, maxHeight);
    }
}
