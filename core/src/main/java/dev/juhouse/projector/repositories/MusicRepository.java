/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.repositories;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import dev.juhouse.projector.dtos.ListMusicDTO;
import dev.juhouse.projector.models.Artist;
import dev.juhouse.projector.models.Music;
import dev.juhouse.projector.other.SQLiteJDBCDriverConnection;

/**
 *
 * @author guilherme
 */
public class MusicRepository {

    private static final int LIST_LIMIT = 500;

    public Music findByNameAndArtist(String name, Artist artist) throws SQLException {
        String sql = "SELECT musics.id as id, musics.name AS name, musics.phrases AS phrases, music_themes.theme_name AS theme " +
                "FROM musics " +
                "LEFT JOIN music_themes ON music_themes.music_id = musics.id " +
                "WHERE name = ? AND artist_id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setString(1, name);

            if (artist == null) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, artist.getIdProperty().getValue());
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Music m = new Music();
                m.setId(rs.getInt("id"));
                m.setName(rs.getString("name"));
                m.setPhrases(Arrays.asList(rs.getString("phrases").split("\n")));
                m.setTheme(rs.getString("theme"));
                if (artist != null) {
                    m.setArtist(new Artist(artist));
                }
                return m;
            }
        }

        return null;
    }

    public void create(Music music) throws SQLException {
        String sql = "INSERT INTO musics(name, artist_id, phrases) VALUES(?, ?, ?)";

        String phrases = music.getPhrasesList()
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
            stmt.setInt(2, music.getArtist().getIdProperty().getValue());
        }

        stmt.setString(3, phrases);
        stmt.execute();
        
        ResultSet keys = stmt.getGeneratedKeys();
        keys.next();
        music.setId(keys.getInt(1));
        saveTheme(music.getId(), music.getTheme());
    }

    public List<ListMusicDTO> listWithLimit() throws SQLException {
        List<ListMusicDTO> result = new ArrayList<>();

        String sql = "SELECT musics.id AS music_id, musics.name AS music_name, musics.phrases AS phrases, "
                + "artists.id AS artist_id, artists.name AS artist_name "
                + "FROM musics "
                + "LEFT JOIN artists ON artists.id = musics.artist_id "
                + "LIMIT " + LIST_LIMIT + ";";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(deserializeDto(rs, null));
            }
        }

        return result;
    }

    public List<ListMusicDTO> listByTermWithLimit(String searchTerm) throws SQLException {
        List<ListMusicDTO> result = new ArrayList<>();

        String sql = "SELECT musics.id AS music_id, musics.name AS music_name, musics.phrases AS phrases, "
                + "artists.id AS artist_id, artists.name AS artist_name "
                + "FROM musics "
                + "LEFT JOIN artists ON artists.id = musics.artist_id "
                + "WHERE musics.name LIKE ? OR artists.name LIKE ? Or musics.phrases LIKE ? "
                + "LIMIT " + LIST_LIMIT + ";";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm + "%");
            stmt.setString(2, "%" + searchTerm + "%");
            stmt.setString(3, "%" + searchTerm + "%");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(deserializeDto(rs, searchTerm));
            }
        }

        return result;
    }

    public Music findById(Integer id) throws SQLException {
        String sql = "SELECT musics.id AS music_id, musics.name AS music_name, musics.phrases AS phrases, "
                + "artists.id AS artist_id, artists.name AS artist_name, "
                + "music_themes.theme_name AS music_theme "
                + "FROM musics "
                + "LEFT JOIN artists ON artists.id = musics.artist_id "
                + "LEFT JOIN music_themes ON music_themes.music_id = musics.id "
                + "WHERE musics.id = ?";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return deserialize(rs);
            }
        }

        return null;
    }

    public void update(Music m) throws SQLException {
        String sql = "UPDATE musics SET name = ?, artist_id = ?, phrases = ? WHERE id = ?";

        String phrases = m.getPhrasesList()
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
            stmt.setInt(2, m.getArtist().getIdProperty().getValue());
        }

        stmt.setString(3, phrases);
        stmt.setInt(4, m.getId());
        stmt.execute();
        saveTheme(m.getId(), m.getTheme());
    }

    private void saveTheme(Integer musicId, String themeName) throws SQLException {
        String delSql = "DELETE FROM music_themes WHERE music_id = ?";

        try (PreparedStatement delStmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(delSql)) {
            delStmt.setInt(1, musicId);
            delStmt.execute();
        }

        if (themeName != null) {
            String themeSql = "INSERT INTO music_themes(music_id, theme_name) VALUES (?, ?)";

            try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                    .getConn()
                    .prepareStatement(themeSql)) {
                stmt.setInt(1, musicId);
                stmt.setString(2, themeName);
                stmt.execute();
            }
        }
    }

    private Music deserialize(ResultSet rs) throws SQLException {
        Music m = new Music();

        int artistId = rs.getInt("artist_id");

        if (!rs.wasNull()) {
            Artist a = new Artist();
            a.getIdProperty().setValue(artistId);
            a.getNameProperty().setValue(rs.getString("artist_name"));
            m.setArtist(a);
        }

        m.setId(rs.getInt("music_id"));
        m.setName(rs.getString("music_name"));
        m.setPhrases(rs.getString("phrases"));
        m.setTheme(rs.getString("music_theme"));
        return m;
    }

    private ListMusicDTO deserializeDto(ResultSet rs, String searchingTerm) throws SQLException {
        ListMusicDTO m = new ListMusicDTO();

        String artistName = rs.getString("artist_name");

        if (rs.wasNull()) {
            m.setArtistName(null);
        } else {
            m.setArtistName(artistName);
        }

        m.setId(rs.getInt("music_id"));
        m.setName(rs.getString("music_name"));

        String[] phrases = rs.getString("phrases").split("\n");
        String firsts;

        if (searchingTerm == null) {
            firsts = Arrays.stream(phrases).limit(5).collect(Collectors.joining("\n"));
        } else {
            final String searchingTermLower = searchingTerm.toLowerCase();

            firsts = Arrays.stream(phrases)
                    .filter(s -> s.toLowerCase().contains(searchingTermLower))
                    .limit(5)
                    .collect(Collectors.joining("\n"));
        }

        m.setPhrases(firsts);

        return m;
    }
}
