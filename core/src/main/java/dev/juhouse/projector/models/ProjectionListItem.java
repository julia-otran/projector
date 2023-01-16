package dev.juhouse.projector.models;

import lombok.Data;
import dev.juhouse.projector.enums.ProjectionListItemType;
import dev.juhouse.projector.other.Identifiable;

@Data
public class ProjectionListItem implements Identifiable {
    private long id;
    private String title;
    private int order;

    private ProjectionListItemType type;

    public String getIdentity() {
        return Long.toString(id);
    }
}
