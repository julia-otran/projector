/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.repositories;

import us.guihouse.projector.models.Artist;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author guilherme
 */
public class ArtistRepository {

    public Artist findById(int id) throws SQLException {
        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("SELECT id, name FROM artists WHERE id = ?");

        try {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Artist a = new Artist();
                a.setId(rs.getInt("id"));
                a.setName(rs.getString("name"));
                return a;
            }
        } finally {
            stmt.close();
        }

        return null;
    }

    public List<Artist> findAll() {
        List<Artist> result = new ArrayList<>();
        try {
            PreparedStatement stmt = SQLiteJDBCDriverConnection
                    .getConn()
                    .prepareStatement("SELECT id, name FROM artists;");

            try {
                ResultSet rs = stmt.executeQuery();
                Artist a;

                while (rs.next()) {
                    a = new Artist();
                    a.setId(rs.getInt("id"));
                    a.setName(rs.getString("name"));
                    result.add(a);
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(MusicLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public Artist findByName(String name) throws SQLException {
        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("SELECT id, name FROM artists WHERE name = ?");

        try {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Artist a = new Artist();
                a.setId(rs.getInt("id"));
                a.setName(rs.getString("name"));
                return a;
            }
        } finally {
            stmt.close();
        }

        return null;
    }

    public Artist findOrCreateByName(String name) throws SQLException {
        Artist a = findByName(name);

        if (a != null) {
            return a;
        }

        a = new Artist();

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("INSERT INTO artists (name) VALUES (?)");

        try {
            stmt.setString(1, name);
            stmt.execute();

            ResultSet generated = stmt.getGeneratedKeys();

            if (generated.next()) {
                a.setId(generated.getInt(1));
                a.setName(name);
                return a;
            }

            throw new SQLException();
        } finally {
            stmt.close();
        }
    }
}
