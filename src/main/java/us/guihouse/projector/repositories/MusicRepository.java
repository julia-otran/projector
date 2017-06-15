/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.repositories;

import us.guihouse.projector.models.Artist;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author guilherme
 */
public class MusicRepository {

    public Music findByNameAndArtist(String name, Artist artist) throws SQLException {
        String sql = "SELECT id, name, phrases FROM musics WHERE name = ? AND artist_id = ?";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setString(1, name);

            if (artist == null) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, artist.getId());
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Music m = new Music();
                m.setId(rs.getInt("id"));
                m.setName(rs.getString("name"));
                m.setPhrases(Arrays.asList(rs.getString("phrases").split("\n")));
                m.setArtist(new Artist(artist));
                return m;
            }
        } finally {
            stmt.close();
        }

        return null;
    }

    public void create(Music music) throws SQLException {
        String sql = "INSERT INTO musics(name, artist_id, phrases) VALUES(?, ?, ?)";

        String phrases = music.getPhrases()
                .stream()
                .map(l -> l.replace("\r\n", "").replace("\n", ""))
                .collect(Collectors.joining("\n"));

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        stmt.setString(1, music.getName());

        if (music.getArtist() == null) {
            stmt.setNull(2, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(2, music.getArtist().getId());
        }

        stmt.setString(3, phrases);
        stmt.execute();
    }

    public List<Music> listAll() throws SQLException {
        List<Music> result = new ArrayList<>();

        String sql = "SELECT musics.id AS music_id, musics.name AS music_name, musics.phrases AS phrases, "
                + "artists.id AS artist_id, artists.name AS artist_name "
                + "FROM musics "
                + "LEFT JOIN artists ON artists.id = musics.artist_id ";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(deserialize(rs));
            }
        } finally {
            stmt.close();
        }

        return result;
    }

    public List<Music> listByTerm(String searchTerm) throws SQLException {
        List<Music> result = new ArrayList<>();

        String sql = "SELECT musics.id AS music_id, musics.name AS music_name, musics.phrases AS phrases, "
                + "artists.id AS artist_id, artists.name AS artist_name "
                + "FROM musics "
                + "LEFT JOIN artists ON artists.id = musics.artist_id "
                + "WHERE musics.name LIKE ? OR artists.name LIKE ?";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setString(1, "%" + searchTerm + "%");
            stmt.setString(2, "%" + searchTerm + "%");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(deserialize(rs));
            }
        } finally {
            stmt.close();
        }

        return result;
    }

    private Music deserialize(ResultSet rs) throws SQLException {
        Music m = new Music();

        int artistId = rs.getInt("artist_id");

        if (!rs.wasNull()) {
            Artist a = new Artist();
            a.setId(artistId);
            a.setName(rs.getString("artist_name"));
            m.setArtist(a);
        }

        m.setId(rs.getInt("music_id"));
        m.setName(rs.getString("music_name"));
        m.setPhrases(Arrays.asList(rs.getString("phrases").split("\n")));

        return m;
    }

    public Music findById(Integer id) throws SQLException {
        String sql = "SELECT musics.id AS music_id, musics.name AS music_name, musics.phrases AS phrases, "
                + "artists.id AS artist_id, artists.name AS artist_name "
                + "FROM musics "
                + "LEFT JOIN artists ON artists.id = musics.artist_id "
                + "WHERE musics.id = ?";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return deserialize(rs);
            }
        } finally {
            stmt.close();
        }

        return null;
    }

    public void update(Music m) throws SQLException {
        String sql = "UPDATE musics SET name = ?, artist_id = ?, phrases = ? WHERE id = ?";

        String phrases = m.getPhrases()
                .stream()
                .map(l -> l.replace("\r\n", "").replace("\n", ""))
                .collect(Collectors.joining("\n"));

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        stmt.setString(1, m.getName());

        if (m.getArtist() == null) {
            stmt.setNull(2, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(2, m.getArtist().getId());
        }

        stmt.setString(3, phrases);
        stmt.setInt(4, m.getId());
        stmt.execute();
    }
}
