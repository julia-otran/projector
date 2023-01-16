/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection.text;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author guilherme
 */
public class NoTextWrapper implements TextWrapper {

    @Override
    public List<WrappedText> fitGroups(List<String> phrases) {
        return phrases.stream()
                .map(p -> new WrappedText(Collections.singletonList(p), 0))
                .collect(Collectors.toList());
    }

}
