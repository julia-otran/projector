package us.guihouse.projector.models;

import lombok.Data;

import java.util.List;

@Data
public class ProjectionList extends SimpleProjectionList {
    private List<ProjectionListItem> items;
}
