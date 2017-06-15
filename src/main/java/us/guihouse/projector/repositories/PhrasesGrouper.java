/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.repositories;

import us.guihouse.projector.models.Music;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrappedText;
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
