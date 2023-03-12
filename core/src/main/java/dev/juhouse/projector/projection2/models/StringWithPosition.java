/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection2.models;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
@Data
@AllArgsConstructor
public class StringWithPosition {
    private final int x;
    private final int y;

    private final int w;

    private final int h;

    private final String text;
}
