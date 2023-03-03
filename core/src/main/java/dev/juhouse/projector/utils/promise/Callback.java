/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.utils.promise;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface Callback<OUT> {
    void success(OUT out);
    void error(Object obj);
}
