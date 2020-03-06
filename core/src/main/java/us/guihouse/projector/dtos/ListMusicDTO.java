/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.dtos;

import lombok.Data;

/**
 *
 * @author guilherme
 */
@Data
public class ListMusicDTO {
    private int id;
    private String name;
    private String artistName;
    private String phrases;
}
