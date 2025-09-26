/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.repositories;

import dev.juhouse.projector.models.Artist;
import dev.juhouse.projector.other.SQLiteJDBCDriverConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ArtistRepository {

    public List<Artist> findAll() {
        List<Artist> result = new ArrayList<>();
        try {

            try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                    .getConn()
                    .prepareStatement("SELECT id, name FROM artists;")) {
                ResultSet rs = stmt.executeQuery();
                Artist a;

                while (rs.next()) {
                    a = new Artist();
                    a.getIdProperty().setValue(rs.getInt("id"));
                    a.getNameProperty().setValue(rs.getString("name"));
                    result.add(a);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(ArtistRepository.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public Artist findByName(String name) throws SQLException {

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("SELECT id, name FROM artists WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Artist a = new Artist();
                a.getIdProperty().setValue(rs.getInt("id"));
                a.getNameProperty().setValue(rs.getString("name"));
                return a;
            }
        }

        return null;
    }

    public Artist findOrCreateByName(String name) throws SQLException {
        Artist a = findByName(name);

        if (a != null) {
            return a;
        }

        a = new Artist();

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement("INSERT INTO artists (name) VALUES (?)")) {
            stmt.setString(1, name);
            stmt.execute();

            PreparedStatement idQueryStmt = SQLiteJDBCDriverConnection
                    .getConn()
                    .prepareStatement("SELECT last_insert_rowid();");

            ResultSet keys = idQueryStmt.executeQuery();

            if (keys.next()) {
                a.getIdProperty().setValue(keys.getInt(1));
                a.getNameProperty().setValue(name);
                return a;
            }

            throw new SQLException();
        }
    }

    public List<Artist> search(String term) {
        List<Artist> result = new ArrayList<>();

        try {

            try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                    .getConn()
                    .prepareStatement("SELECT id, name FROM artists WHERE name LIKE ?;")) {

                stmt.setString(1, "%" + term + "%");

                ResultSet rs = stmt.executeQuery();
                Artist a;

                while (rs.next()) {
                    a = new Artist();
                    a.getIdProperty().setValue(rs.getInt("id"));
                    a.getNameProperty().setValue(rs.getString("name"));
                    result.add(a);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(ArtistRepository.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
}
