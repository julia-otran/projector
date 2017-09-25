package us.guihouse.projector.other;

import javafx.embed.swing.SwingNode;

/**
 * Created by guilherme on 24/09/17.
 */
public class ResizeableSwingNode extends SwingNode {
    @Override
    public boolean isResizable() {
        return false;
    }
}
