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
    
    @Override
    protected List<String> splitIntoLines(String str) {
        List<String> strings = new ArrayList<>();
        
        super.splitIntoLines(str)
            .forEach(line -> {
                int countCommas = countCommas(line);
                if (countCommas > 0 && countCommas <= 2) {
                    StringBuilder builder = new StringBuilder();

                    int halfCommas = countCommas / 2;
                    int current = 0;
                    int l = line.length();
                    for (int i = 0; i<l; i++) {
                        char c = line.charAt(i);
                        builder.append(c);
                        if (c == ',') {
                            current++;
                        }
                        if ((countCommas == 1 && current == 1) ||
                            (halfCommas > 0 && current >= halfCommas)) {
                            current = 0;
                            strings.add(builder.toString());
                            builder = new StringBuilder();
                        }
                    }

                    if (builder.length() > 0) {
                        strings.add(builder.toString());
                    }
                } else {
                    strings.add(line);
                }
            });
    
        return strings.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    private int countCommas(String s) {
        int l = s.length();
        int c = 0;
        
        for (int i=0; i<l; i++) {
            if (s.charAt(i) == ',') {
                c++;
            }
        }

        return c;
    }
}
