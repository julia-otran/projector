/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.text;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * TODO: Remove multiline wrapper
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class SingleLineTextWrapper extends MultilineTextWrapper {

    SingleLineTextWrapper(List<TextWrapperMetrics> metrics) {
        super(metrics);
    }
}
