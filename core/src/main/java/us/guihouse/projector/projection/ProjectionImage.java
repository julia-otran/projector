/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.projection;

import us.guihouse.projector.projection.models.BackgroundModel;
import us.guihouse.projector.projection.models.BackgroundProvide;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 *
 * @author guilherme
 */
public class ProjectionImage implements Projectable {

    protected CanvasDelegate canvasDelegate;

    private final Color bgColor;

    private BufferedImage scaledBackground;
    private BufferedImage scaledOverlay;
    private BufferedImage scaledLogo;

    private int overlayCenter;
    private double backgroundScale;

    private boolean cropBackground;
    private boolean enableAnimation = false;
    private BackgroundProvide model;

    private int xDelta = 0;
    private float deltaRate = 0;
    private float stable = 0;
    private int step = 0;

    ProjectionImage(CanvasDelegate canvasDelegate) {
        this(canvasDelegate, new Color(0, 0, 0));
    }

    public ProjectionImage(CanvasDelegate canvasDelegate, Color bgColor) {
        this.canvasDelegate = canvasDelegate;
        this.bgColor = bgColor;
    }

    public boolean hasImage() {
        return scaledBackground != null || scaledOverlay != null || scaledLogo != null;
    }

    @Override
    public void paintComponent(Graphics2D g) {
        if (!hasImage()) {
            return;
        }

        g.setColor(bgColor);
        g.fillRect(0, 0, canvasDelegate.getWidth(), canvasDelegate.getHeight());

        if (scaledBackground != null) {
            g.drawImage(scaledBackground, 0, 0, null);
        }

        if (scaledOverlay != null) {
            generateDelta();

            Composite old = g.getComposite();

            float alpha = 0.5f;

            AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g.setComposite(composite);

            int dx2 = canvasDelegate.getWidth();
            int dy2 = canvasDelegate.getHeight();

            int sx1;
            int sx2;

            if (enableAnimation) {
                sx1 = overlayCenter + xDelta;
                sx2 = overlayCenter + xDelta + canvasDelegate.getWidth();
            } else {
                sx1 = overlayCenter;
                sx2 = overlayCenter + canvasDelegate.getWidth();
            }

            int sy2 = canvasDelegate.getHeight();

            g.drawImage(scaledOverlay, 0, 0, dx2, dy2, sx1, 0, sx2, sy2, null);
            g.setComposite(old);
        }

        if (scaledLogo != null) {
            g.drawImage(scaledLogo, 0, 0, null);
        }
    }

    private void generateDelta() {
        if (step > 40) {
            deltaRate = (float) ((Math.random() * 0.025) - 0.0125);
            step = -40;
            stable = -1 * Math.round(deltaRate * step * step);
        }

        step++;

        xDelta = Math.round(stable + (deltaRate * step * step));
    }

    @Override
    public CanvasDelegate getCanvasDelegate() {
        return canvasDelegate;
    }

    @Override
    public void rebuildLayout() {
        scaledBackground = null;
        scaledOverlay = null;
        scaledLogo = null;

        if (model == null || BackgroundModel.Type.NONE.equals(model.getType())) {
            return;
        }

        if (BackgroundModel.Type.STATIC.equals(model.getType())) {
            if (model.getStaticBackground() != null) {
                scaleBackground(model.getStaticBackground());
            }
        } else {
            if (model.getBackground() != null) {
                scaleBackground(model.getBackground());
            }

            if (model.getLogo() != null) {
                scaleLogo();
            }

            if (model.getOverlay() != null) {
                scaleOverlay();
            }
        }
    }

    private void scaleBackground(BufferedImage img) {
        double imgW = img.getWidth();
        double imgH = img.getHeight();

        int width = canvasDelegate.getWidth();
        int height = canvasDelegate.getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        double scaleX = width / imgW;
        double scaleY = height / imgH;

        if (cropBackground) {
            backgroundScale = Math.max(scaleX, scaleY);
        } else {
            backgroundScale = Math.min(scaleX, scaleY);
        }

        int newW = (int) Math.round(imgW * backgroundScale);
        int newH = (int) Math.round(imgH * backgroundScale);

        int x = (width - newW) / 2;
        int y = (height - newH) / 2;

        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.scale(backgroundScale, backgroundScale);

        BufferedImage scaling = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaling.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setTransform(at);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        this.scaledBackground = scaling;
    }

    private void scaleLogo() {
        BufferedImage img = model.getLogo();
        double imgW = img.getWidth();
        double imgH = img.getHeight();

        int width = canvasDelegate.getWidth();
        int height = canvasDelegate.getHeight();

        int newW = (int) Math.round(imgW * backgroundScale);
        int newH = (int) Math.round(imgH * backgroundScale);

        int x = (width - newW) / 2;
        int y = (height - newH) / 2;

        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.scale(backgroundScale, backgroundScale);

        BufferedImage scaling = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaling.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setTransform(at);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        this.scaledLogo = scaling;
    }

    private void scaleOverlay() {
        BufferedImage img = model.getOverlay();
        double imgW = img.getWidth();
        double imgH = img.getHeight();

        int width = canvasDelegate.getWidth();
        int height = canvasDelegate.getHeight();

        int newW = (int) Math.round(imgW * backgroundScale);
        int newH = (int) Math.round(imgH * backgroundScale);

        int y = (height - newH) / 2;

        AffineTransform at = new AffineTransform();
        at.translate(0, y);
        at.scale(backgroundScale, backgroundScale);

        BufferedImage scaling = new BufferedImage(newW, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaling.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setTransform(at);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        this.overlayCenter = (newW - width) / 2;
        this.scaledOverlay = scaling;
    }

    @Override
    public void init() {
        rebuildLayout();
    }

    @Override
    public void finish() {

    }

    public void setCropBackground(boolean cropBackground) {
        if (this.cropBackground != cropBackground) {
            this.cropBackground = cropBackground;
            rebuildLayout();
        }
    }

    public BackgroundProvide getModel() {
        return model;
    }

    public void setModel(BackgroundProvide model) {
        this.model = model;
        rebuildLayout();
    }

    public boolean isEnableAnimation() {
        return enableAnimation;
    }

    public void setEnableAnimation(boolean enableAnimation) {
        this.enableAnimation = enableAnimation;
    }
}
