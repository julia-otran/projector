/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.projection;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 *
 * @author guilherme
 */
public class ProjectionCanvas extends JComponent implements CanvasDelegate {

    private final List<Projectable> projectables;

    public ProjectionCanvas() {
        projectables = new ArrayList<>();
    }

    public void addProjectable(Projectable p) {
        projectables.add(p);
        p.init(this);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        projectables.stream().forEach((p) -> {
            p.paintComponent(g);
        });
    }

    @Override
    public void revalidate() {
        super.revalidate();
        for (Projectable p : projectables) {
            p.rebuildLayout();
        }
    }
}
