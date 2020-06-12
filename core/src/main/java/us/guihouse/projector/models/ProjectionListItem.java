package us.guihouse.projector.models;

import lombok.Data;
import us.guihouse.projector.enums.ProjectionListItemType;
import us.guihouse.projector.other.Identifiable;

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
