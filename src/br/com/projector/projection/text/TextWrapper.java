/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection.text;

import java.awt.FontMetrics;
import java.util.List;

/**
 *
 * @author guilherme
 */
public interface TextWrapper {
    List<WrappedText> fitGroups(List<String> phrases);
    
    int getMaxWidth();
    int getMaxHeight();
    FontMetrics getFontMetrics();
}
