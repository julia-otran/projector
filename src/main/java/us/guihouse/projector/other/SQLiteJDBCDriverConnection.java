/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other;

import us.guihouse.projector.other.file_filters.DatabaseFileFilter;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

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

        FileChooser chooser;

        do {
            boolean openFile = shouldOpenFile();

            chooser = new FileChooser();
            
            chooser.getExtensionFilters().setAll(DatabaseFileFilter.getFilter());
            chooser.setInitialFileName("musicas.db");

            if (openFile) {
                current = chooser.showOpenDialog(null);
            } else {
                current = chooser.showSaveDialog(null);
            }

            if (current == null) {
                throw new RuntimeException("Cannot proceed without database");
            }
            
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
        } while (current == null);

        return current.getAbsolutePath();
    }

    private static boolean shouldOpenFile() {
        Alert d = new Alert(AlertType.CONFIRMATION);
        d.setTitle("Inicializar dados");
        d.setHeaderText("Começar com uma nova bibliotaca de letras?");
        d.setContentText("Você pode escolher entre começar sem nenhuma letra, ou abrir uma biblioteca de letras já existente.");
        
        ButtonType btOpen = new ButtonType("Abrir Biblioteca", ButtonBar.ButtonData.OTHER);
        ButtonType btInit = new ButtonType("Iniciar do zero", ButtonBar.ButtonData.OTHER);
        
        d.getDialogPane().getButtonTypes().setAll(btOpen, btInit);
        
        Optional<ButtonType> result = d.showAndWait();
        
        if (result.isPresent() && btOpen.equals(result.get())) {
            return true;
        }
        
        if (result.isPresent() && btInit.equals(result.get())) {
            return false;
        }
        
        throw new RuntimeException("Cannot proceed without database");
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
                    + "FOREIGN KEY(artist_id) REFERENCES artists(id)"
                    + ");";

            stmt.execute(query);
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS UQ_MUSIC ON musics(artist_id, name);");

            query = "CREATE TABLE IF NOT EXISTS music_themes("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "music_id INTEGER NOT NULL, "
                    + "theme_name VARCHAR NOT NULL, "
                    + "FOREIGN KEY(music_id) REFERENCES musics(id) ON DELETE CASCADE"
                    + ")";
            stmt.execute(query);

            query = "CREATE TABLE IF NOT EXISTS musics_plays("
                    + "music_id INTEGER, "
                    + "date DATETIME, "
                    + "FOREIGN KEY(music_id) REFERENCES musics(id) ON DELETE CASCADE"
                    + ");";

            stmt.execute(query);

            query = "CREATE TABLE IF NOT EXISTS projection_lists("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title VARCHAR NOT NULL, "
                    + "active INTEGER NOT NULL DEFAULT 1"
                    + ")";
            stmt.execute(query);

            query = "CREATE TABLE IF NOT EXISTS projection_list_items("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title VARCHAR NOT NULL, "
                    + "type VARCHAR NOT NULL, "
                    + "projection_list_id INTEGER NOT NULL, "
                    + "order_number INTEGER NOT NULL, "
                    + "FOREIGN KEY(projection_list_id) REFERENCES projection_lists(id) ON DELETE CASCADE"
                    + ")";
            stmt.execute(query);

            query = "CREATE TABLE IF NOT EXISTS projection_list_item_properties("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "key VARCHAR NOT NULL, "
                    + "value VARCHAR NOT NULL, "
                    + "projection_list_item_id INTEGER NOT NULL, "
                    + "FOREIGN KEY(projection_list_item_id) REFERENCES projection_list_items(id) ON DELETE CASCADE"
                    + ")";
            stmt.execute(query);

            stmt.close();

        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBCDriverConnection.class.getName()).log(Level.SEVERE, null, ex);
            conn = null;
        }
    }
}
;