/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.projection;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;

import dev.juhouse.projector.projection.models.VirtualScreen;
import lombok.Getter;
import dev.juhouse.projector.other.ProjectorPreferences;

/**
 *
 * @author guilherme
 */
public class ProjectionLabelBackground implements Projectable {

    private final CanvasDelegate canvasDelegate;

    @Getter
    private boolean darkenBackground;

    private final HashMap<String, Boolean> actives = new HashMap<>();

    private static final Color OVERLAY = new Color(0, 0, 0, 210);

    private final HashMap<String, PaintableCrossFader> faders = new HashMap<>();
    private boolean show = false;

    public ProjectionLabelBackground(CanvasDelegate canvasDelegate) {
        this.canvasDelegate = canvasDelegate;
        darkenBackground = ProjectorPreferences.getDarkenBackground();

    }

    @Override
    public void init() {
        rebuildLayout();
    }

    @Override
    public void finish() {
    }

    public void setDarkenBackground(boolean darken) {
        this.darkenBackground = darken;
        ProjectorPreferences.setDarkenBackground(darken);
        setupFaders();
    }

    public void setShow(boolean show) {
        if (show != this.show) {
            this.show = show;
            setupFaders();
        }
    }

    @Override
    public void paintComponent(Graphics2D g, VirtualScreen vs) {
        PaintableCrossFader fader = faders.get(vs.getVirtualScreenId());

        if (fader != null) {
            fader.paintComponent(g);
        }
    }

    @Override
    public void rebuildLayout() {
        faders.clear();
        actives.clear();

        canvasDelegate.getVirtualScreens().forEach(vs -> {
            PaintableCrossFader fader = new PaintableCrossFader(vs);
            fader.setStepPerFrame(0.1f);
            faders.put(vs.getVirtualScreenId(), fader);
        });

        setupFaders();
    }

    private void setupFaders() {
        faders.forEach((screenId, fader) -> {
            if (!fader.getScreen().isChromaScreen()) {
                if (darkenBackground && show) {
                    actives.put(screenId, true);
                    fader.fadeIn(new BackgroundPaintable());
                } else {
                    if (actives.getOrDefault(screenId, false)) {
                        actives.put(screenId, false);
                        fader.fadeOut();
                    }
                }
            }
        });
    }

    static class BackgroundPaintable implements Paintable {
        @Override
        public void paintComponent(Graphics2D g, VirtualScreen vs) {
            g.setColor(OVERLAY);
            g.fillRect(0, 0, vs.getWidth(), vs.getHeight());
        }
    }
}
