/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms;

import us.guihouse.projector.forms.*;
import us.guihouse.projector.forms.ManageMusicFrame.SaveCallback;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.repositories.MusicRepository;
import us.guihouse.projector.repositories.MusicTableModel;
import us.guihouse.projector.repositories.OpenMusicRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author guilherme
 */
public class ListMusicsFrame extends javax.swing.JFrame {

    private final OpenMusicRepository openMusicRepo;
    private final MusicRepository musicRepository;
    private final MusicTableModel tableModel;
    private final List<Music> musics;

    /**
     * Creates new form ListMusicsFrame
     */
    public ListMusicsFrame(OpenMusicRepository openMusicRepo) {
        initComponents();

        this.openMusicRepo = openMusicRepo;
        this.musicRepository = new MusicRepository();
        this.tableModel = new MusicTableModel();
        this.jTableMusics.setModel(tableModel);
        this.musics = new ArrayList<>();

        try {
            loadMusics();
        } catch (SQLException ex) {
            Logger.getLogger(ListMusicsFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadMusics() throws SQLException {
        String searchTerm = jTextFieldSearchTerm.getText().trim();

        musics.clear();

        if (searchTerm.isEmpty()) {
            musics.addAll(musicRepository.listAll());
        } else {
            musics.addAll(musicRepository.listByTerm(searchTerm));
        }

        tableModel.setMusics(musics);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableMusics = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldSearchTerm = new javax.swing.JTextField();
        jButtonSearch = new javax.swing.JButton();
        jButtonAddToList = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jTableMusics.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTableMusics);

        jLabel1.setText("Nome/Artista/Letra");

        jButtonSearch.setText("Procurar");
        jButtonSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSearchActionPerformed(evt);
            }
        });

        jButtonAddToList.setText("Adicionar à lista");
        jButtonAddToList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddToListActionPerformed(evt);
            }
        });

        jButtonEdit.setText("Editar Letra");
        jButtonEdit.setToolTipText("");
        jButtonEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEditActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSearchTerm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSearch))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAddToList)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldSearchTerm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSearch))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAddToList)
                    .addComponent(jButtonEdit))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSearchActionPerformed
        try {
            loadMusics();
        } catch (SQLException ex) {
            Logger.getLogger(ListMusicsFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonSearchActionPerformed

    private void jButtonAddToListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddToListActionPerformed
        int selected = jTableMusics.getSelectedRow();

        if (selected >= 0 && selected < musics.size()) {
            Music m = musics.get(selected);
            if (!openMusicRepo.contains(m)) {
                openMusicRepo.add(m);
            }
        }
    }//GEN-LAST:event_jButtonAddToListActionPerformed

    private void jButtonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEditActionPerformed
        int selected = jTableMusics.getSelectedRow();

        if (selected >= 0 && selected < musics.size()) {
            Music m = musics.get(selected);
            ManageMusicFrame mmf = new ManageMusicFrame(m);
            mmf.setVisible(true);
            mmf.setSaveCallback(new SaveCallback() {
                @Override
                public void onSave(Music music) {
                    openMusicRepo.updateMusic(music);
                    try {
                        loadMusics();
                    } catch (SQLException ex) {
                        Logger.getLogger(ListMusicsFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }//GEN-LAST:event_jButtonEditActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddToList;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonSearch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableMusics;
    private javax.swing.JTextField jTextFieldSearchTerm;
    // End of variables declaration//GEN-END:variables
}