/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.text;

import java.awt.FontMetrics;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class MultilineTextWrapper implements TextWrapper {

    private final List<TextWrapperMetrics> metrics;

    MultilineTextWrapper(List<TextWrapperMetrics> metrics) {
        this.metrics = metrics;
    }

    @Override
    public List<WrappedText> fitGroups(List<String> phrases) {
        List<WrappedText> result = new ArrayList<>();

        for (int i = 0; i < phrases.size(); i++) {
            String currentPhrase = phrases.get(i);

            do {
                final String phrase = currentPhrase;
                List<TextWrapperMetricsFitInLine> fits = metrics.stream().map(m -> m.getFitInLines(phrase)).toList();

                int fitsAllPosition = fits.stream().map(TextWrapperMetricsFitInLine::getLineLength).min(Integer::compareTo).orElse(phrase.length());

                Map<Integer, List<String>> fitsAll = fits.stream().map(f -> f.stripAt(fitsAllPosition))
                        .collect(Collectors.toMap(TextWrapperMetricsFitInLine::getRenderId, TextWrapperMetricsFitInLine::getLines));

                result.add(new WrappedText(fitsAll, i));

                currentPhrase = currentPhrase.substring(fitsAllPosition);
            } while (!currentPhrase.isEmpty());
        }

        return result;
    }
}
