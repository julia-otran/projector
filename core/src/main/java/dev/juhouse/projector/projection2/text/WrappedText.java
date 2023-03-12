/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public record WrappedText(@Getter Map<Integer, List<String>> renderLines, @Getter int sourcePhraseNumber) {
    public static WrappedText blankText() {
        return new WrappedText(new HashMap<>(), 0);
    }

    public boolean isEmpty() {
        if (renderLines.isEmpty()) {
            return true;
        }

        return renderLines.values().stream().map(l -> l.isEmpty() || l.get(0).isEmpty()).allMatch(Predicate.isEqual(true));
    }

    public String getJoinedLines() {
        return renderLines.values().stream().findAny().map(lines -> String.join(" ", lines)).orElse("");
    }
}
