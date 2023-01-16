package dev.juhouse.projector.projection;

import dev.juhouse.projector.projection.models.VirtualScreen;

import java.awt.*;

public interface Paintable {
    void paintComponent(Graphics2D g, VirtualScreen vs);
}
