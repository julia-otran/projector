/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.projector.projector.other;

import br.com.projector.projector.other.file_filters.DatabaseFileFilter;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author sqlitetutorial.net
 */
public class SQLiteJDBCDriverConnection {

    private static Connection conn;

    /**
     * Connect to a sample database
     */
    public static void connect() {
        try {
            // create a connection to the database
            String path = getDatabaseUrl();

            if (path == null) {
                return;
            }

            conn = DriverManager.getConnection("jdbc:sqlite:" + path);
            ProjectorPreferences.setSqlitePath(path);
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String getDatabaseUrl() {
        String path = ProjectorPreferences.getSqlitePath();
        File current = new File(path);

        if ((!path.isEmpty()) && current.isFile() && current.canWrite()) {
            return path;
        }

        JFileChooser chooser;

        do {
            int result = shouldOpenFile();

            if (result == JOptionPane.CANCEL_OPTION) {
                return null;
            }

            chooser = new JFileChooser();

            chooser.setFileFilter(new DatabaseFileFilter());
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            if (result == JOptionPane.YES_OPTION) {
                result = chooser.showSaveDialog(null);
            } else {
                result = chooser.showOpenDialog(null);
            }

            if (result == JFileChooser.APPROVE_OPTION) {
                current = chooser.getSelectedFile();

                if (current.isDirectory()) {
                    current = null;
                    continue;
                }

                if (!current.getName().endsWith(".db")) {
                    current = new File(current.getAbsolutePath() + ".db");
                }

                if (current.exists() && !current.canWrite()) {
                    current = null;
                }
            } else {
                return null;
            }
        } while (current == null);

        return current.getAbsolutePath();
    }

    private static int shouldOpenFile() {
        return JOptionPane.showConfirmDialog(null, "Inicializar dados do zero?");
    }

    public static Connection getConn() {
        return conn;
    }

    public static void migrate() {
        if (conn == null) {
            return;
        }

        try {
            Statement stmt = conn.createStatement();

            String query = "CREATE TABLE IF NOT EXISTS artists("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name VARCHAR NOT NULL"
                    + ");";

            stmt.execute(query);
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS UQ_ARTIST_NAME ON artists(name);");

            query = "CREATE TABLE IF NOT EXISTS musics("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name VARCHAR NOT NULL, "
                    + "artist_id INTEGER, "
                    + "phrases TEXT, "
                    + "FOREIGN KEY(artist_id) REFERENCES artists(artist_id)"
                    + ");";

            stmt.execute(query);
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS UQ_MUSIC ON musics(artist_id, name);");

            stmt.close();

        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBCDriverConnection.class.getName()).log(Level.SEVERE, null, ex);
            conn = null;
        }
    }
}
