/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.text;

import java.awt.FontMetrics;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class WrapperFactory {

    private final int maxWidth;
    private final int maxHeight;
    private final FontMetrics fontMetrics;

    public WrapperFactory(int maxWidth, int maxHeight, FontMetrics fontMetrics) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.fontMetrics = fontMetrics;
    }

    public TextWrapper getTextWrapper(boolean multiPhrases) {
        if (fontMetrics == null || maxWidth <= 0 || maxHeight <= 0) {
            return new NoTextWrapper();
        }

        if (multiPhrases) {
            return new MultilineTextWrapper(fontMetrics, maxWidth, maxHeight);
        }

        return new SingleLineTextWrapper(fontMetrics, maxWidth, maxHeight);
    }
}
