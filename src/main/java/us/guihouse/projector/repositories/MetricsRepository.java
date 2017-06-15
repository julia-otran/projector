/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.repositories;

import us.guihouse.projector.models.Music;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.sql.Date;
import java.time.LocalDate;

/**
 *
 * @author guilherme
 */
public class MetricsRepository {
    public void registerPlay(Music music) throws SQLException {
        LocalDate now = LocalDate.now(ZoneOffset.UTC.normalized());
        String sql = "INSERT INTO musics_plays(music_id, date) VALUES (?, ?)";

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);
        try {        
            stmt.setInt(1, music.getId());
            stmt.setDate(2, Date.valueOf(now));

            stmt.execute();
        } finally {
            stmt.close();
        }
    }
}
