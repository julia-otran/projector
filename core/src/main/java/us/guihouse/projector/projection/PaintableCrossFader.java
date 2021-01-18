package us.guihouse.projector.projection;

import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.projection.models.VirtualScreen;
import us.guihouse.projector.projection.video.ProjectionBackgroundVideo;

import java.awt.*;

public class PaintableCrossFader {
    public PaintableCrossFader(VirtualScreen vs) {
        this.screen = vs;
    }

    enum FadeDirection {
        IN, OUT, IN_OUT
    }

    @Getter
    private final VirtualScreen screen;
    private Paintable current;
    private Paintable previous;
    private float currentFadeAlpha;
    private FadeDirection direction = null;

    @Getter
    @Setter
    private float stepPerFrame = 0.05f;

    public void fadeIn(Paintable next) {
        previous = current;
        currentFadeAlpha = 0;
        current = next;
        direction = FadeDirection.IN;
    }

    public void crossFadeIn(Paintable next) {
        previous = current;
        currentFadeAlpha = 0;
        current = next;
        direction = FadeDirection.IN_OUT;
    }

    public void paintComponent(Graphics2D g) {
        if (direction == FadeDirection.IN || direction == FadeDirection.IN_OUT) {
            currentFadeAlpha += stepPerFrame;

            if (Float.compare(currentFadeAlpha, 1.0f) > 0) {
                direction = null;
                currentFadeAlpha = 1.0f;
                previous = null;
            }
        } else if (direction == FadeDirection.OUT){
            currentFadeAlpha -= stepPerFrame;

            if (Float.compare(currentFadeAlpha, 0f) < 0) {
                direction = null;
                currentFadeAlpha = 0f;
                current = null;
            }
        }

        if (previous != null) {
            if (direction == FadeDirection.IN_OUT) {
                Composite old = g.getComposite();
                AlphaComposite fade = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - currentFadeAlpha);
                g.setComposite(fade);
                previous.paintComponent(g, screen);
                g.setComposite(old);
            } else {
                previous.paintComponent(g, screen);
            }
        }

        if (current != null) {
            Composite old = g.getComposite();
            AlphaComposite fade = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentFadeAlpha);
            g.setComposite(fade);
            current.paintComponent(g, screen);
            g.setComposite(old);
        }
    }

    public void fadeOut() {
        direction = FadeDirection.OUT;
        currentFadeAlpha = 1.0f;
        previous = null;
    }
}
