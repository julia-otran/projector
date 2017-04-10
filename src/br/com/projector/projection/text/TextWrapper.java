/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projection.text;

import br.com.projector.projection.models.StringWithWidth;
import java.util.List;

/**
 *
 * @author guilherme
 */
public interface TextWrapper {
    List<StringWithWidth> wrap(String str);
    List<List<String>> fitGroups(List<String> phrases);
}
