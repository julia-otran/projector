/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.repositories;

import br.com.projector.models.Music;
import br.com.projector.projection.text.TextWrapper;
import br.com.projector.projection.text.WrappedText;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author guilherme
 */
public class PhrasesGrouper {

    private TextWrapper wrapper;

    public TextWrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(TextWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public List<WrappedText> groupMusic(Music m) {
        if (wrapper == null) {
            return Collections.emptyList();
        }

        return wrapper.fitGroups(m.getPhrases());
    }

}
