/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.text;

import lombok.AllArgsConstructor;

import java.util.List;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
@AllArgsConstructor
public class WrapperFactory {
    private final List<TextWrapperMetrics> metrics;

    public TextWrapper getTextWrapper(boolean multiPhrases) {
        return new SingleLineTextWrapper(metrics);
    }
}
