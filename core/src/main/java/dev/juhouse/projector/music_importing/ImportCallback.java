/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.music_importing;

import dev.juhouse.projector.dtos.ImportingMusicDTO;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public interface ImportCallback {

    void onImportSuccess(ImportingMusicDTO music);

    void onImportError(boolean errored);
}
