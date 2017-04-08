/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projector;

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;

/**
 *
 * @author 15096134
 */
public class Projector {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice devices[] = ge.getScreenDevices();
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ProjectionFrame projectionFrame = new ProjectionFrame();
                projectionFrame.setVisible(false);
                
                MainFrame controlFrame = new MainFrame(projectionFrame);
                controlFrame.setVisible(false);
               
                // Prevent panes from displaying in wrong screen
                JOptionPane.setRootFrame(controlFrame);
               
                GraphicsDevice dev = findProjectionDevice(devices);
                // When no device to output, no output.
                if (dev != null) {
                    Rectangle bounds = dev.getDefaultConfiguration().getBounds();
                    projectionFrame.setBounds(bounds);
                    projectionFrame.setVisible(true);
                }
                
                controlFrame.setVisible(true);
            }
        });
    }
    
    private static GraphicsDevice findProjectionDevice(GraphicsDevice allDevices[]) {
        List<GraphicsDevice> devices = Arrays.asList(allDevices)
                .stream()
                .filter(GraphicsDevice::isFullScreenSupported)
                .collect(Collectors.toList());
        
        long count = devices.size();
        if (count <= 0) {
            return null;
        }
        
        Object names[] = devices.stream().map(GraphicsDevice::getIDstring).toArray();
        int selected = JOptionPane.showOptionDialog(null, "Em qual tela a letra deve ser projetada?", "Selecionar tela", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, names, null);
        
        if (selected < 0 || selected >= count) {
            return null;
        } 
        
        return devices.get(selected);
    }
}
