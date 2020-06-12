package us.guihouse.projector.projection;

import us.guihouse.projector.projection.models.VirtualScreen;

import java.awt.*;

public interface Paintable {
    void paintComponent(Graphics2D g, VirtualScreen vs);
}
