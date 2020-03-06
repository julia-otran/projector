/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.utils.promise;

/**
 *
 * @author guilherme
 */
public interface Callback<OUT> {
    public void success(OUT out);
    public void error(Object obj);
}
