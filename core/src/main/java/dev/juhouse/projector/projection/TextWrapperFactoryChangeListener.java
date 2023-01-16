/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection;

import dev.juhouse.projector.projection.text.WrapperFactory;

/**
 *
 * @author guilherme
 */
public interface TextWrapperFactoryChangeListener {

    void onWrapperFactoryChanged(WrapperFactory factory);
}
