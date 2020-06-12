/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection.text;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author guilherme
 */
public class WrappedText {

    private final List<String> lines;

    WrappedText(List<String> lines) {
        this.lines = Collections.unmodifiableList(lines);
    }

    public static WrappedText blankText() {
        return new WrappedText(Collections.singletonList(" "));
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean isEmpty() {
        if (lines.size() == 1) {
            return lines.get(0).trim().isEmpty();
        }

        return lines.isEmpty();
    }

    public String getJoinedLines() {
        return String.join(" ", lines);
    }
}
