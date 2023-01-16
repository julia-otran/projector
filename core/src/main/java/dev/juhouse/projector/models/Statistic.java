package dev.juhouse.projector.models;

import lombok.Data;

@Data
public class Statistic {
    private Music music;
    private Integer musicId;
    private Integer playCount;
}
