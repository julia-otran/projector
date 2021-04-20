package us.guihouse.projector.projection;

import lombok.Getter;
import lombok.Setter;
import us.guihouse.projector.projection.glfw.GLFWGraphicsAdapter;
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
    private Runnable previousCallback = null;
    private Runnable callback = null;

    @Getter
    @Setter
    private float stepPerFrame = 0.05f;

    @Getter
    @Setter
    private boolean cascadeFade = false;

    public void fadeIn(Paintable next) {
        previous = current;
        currentFadeAlpha = 0f;
        current = next;
        direction = FadeDirection.IN;
    }

    public void crossFadeIn(Paintable next, Runnable callback) {
        if (this.previousCallback != null) {
            this.previousCallback.run();
        }

        previousCallback = this.callback;
        this.callback = callback;
        previous = current;
        currentFadeAlpha = 0f;
        current = next;
        direction = FadeDirection.IN_OUT;
    }

    public void paintComponent(GLFWGraphicsAdapter g) {
        if (direction == FadeDirection.IN || direction == FadeDirection.IN_OUT) {
            currentFadeAlpha += stepPerFrame;

            if (currentFadeAlpha > 1.0f) {
                direction = null;
                currentFadeAlpha = 1.0f;
                previous = null;

                if (previousCallback != null) {
                    previousCallback.run();
                    previousCallback = null;
                }
            }
        } else if (direction == FadeDirection.OUT){
            currentFadeAlpha -= stepPerFrame;

            if (currentFadeAlpha < 0f) {
                direction = null;
                currentFadeAlpha = 0f;
                current = null;

                if (callback != null) {
                    callback.run();
                    callback = null;
                }
            }
        }

        if (previous != null) {
            if (direction == FadeDirection.IN_OUT) {
                float oldAlpha = g.getAlpha();

                float cascadeComposite = 1.0f;

                if (cascadeFade) {
                    cascadeComposite = oldAlpha;
                }

                cascadeComposite *= 1.0f - currentFadeAlpha;

                g.setAlpha(cascadeComposite);
                previous.paintComponent(g, screen);
                g.setAlpha(oldAlpha);
            } else {
                previous.paintComponent(g, screen);
            }
        }

        if (current != null) {
            float cascadeComposite = 1.0f;
            float oldAlpha = g.getAlpha();

            if (cascadeFade) {
                cascadeComposite = oldAlpha;
            }

            cascadeComposite *= currentFadeAlpha;

            g.setAlpha(cascadeComposite);
            current.paintComponent(g, screen);
            g.setAlpha(oldAlpha);
        }
    }

    public void fadeOut() {
        direction = FadeDirection.OUT;
        currentFadeAlpha = 1.0f;
        previous = null;
    }
}
