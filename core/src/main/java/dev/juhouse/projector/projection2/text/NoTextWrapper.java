/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.text;

import dev.juhouse.projector.projection2.BridgeRender;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
@AllArgsConstructor
public class NoTextWrapper implements TextWrapper {
    private final List<Integer> renderIds;

    @Override
    public List<WrappedText> fitGroups(List<String> phrases) {

        return phrases.stream()
                .map(p -> {
                    Map<Integer, List<String>> lines = new HashMap<>();

                    renderIds.forEach(id -> lines.put(id, Collections.singletonList(p)));

                    return new WrappedText(lines, 0);
                })
                .collect(Collectors.toList());
    }

}
