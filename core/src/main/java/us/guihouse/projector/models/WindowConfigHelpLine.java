package us.guihouse.projector.models;

import lombok.Data;

@Data
public class WindowConfigHelpLine {
    private Integer X1;
    private Integer X2;
    private Integer Y1;
    private Integer Y2;

    public Integer getX1() {
        return X1;
    }

    public Integer getX2() {
        return X2;
    }

    public Integer getY1() {
        return Y1;
    }

    public Integer getY2() {
        return Y2;
    }
}
