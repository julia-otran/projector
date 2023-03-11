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
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class SingleLineTextWrapper extends MultilineTextWrapper {
    public SingleLineTextWrapper(FontMetrics fontMetrics, int maxWidth, int maxHeight) {
        super(fontMetrics, maxWidth, maxHeight);
    }

    @Override
    public List<WrappedText> fitGroups(List<String> phrases) {
        List<WrappedText> groups = new ArrayList<>();
        
        for (int i = 0; i < phrases.size(); i++) {
            String phrase = phrases.get(i);
            WrappedText wrapped = wrap(phrase);

            List<String> building = new ArrayList<>();

            for (String wrappedPhrase : wrapped.getLines()) {
                building.add(wrappedPhrase);

                if (building.size() >= lineLimit) {
                    groups.add(new WrappedText(building, i));
                    building = new ArrayList<>();
                }
            }

            groups.add(new WrappedText(building, i));
        }
        
        return groups;
    }
}
