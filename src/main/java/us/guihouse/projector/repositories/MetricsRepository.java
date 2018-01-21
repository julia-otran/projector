/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.repositories;

import us.guihouse.projector.enums.IntervalChoice;
import us.guihouse.projector.enums.Weekday;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.models.Statistic;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author guilherme
 */
public class MetricsRepository {
    private static final int STATISTICS_LIMIT = 15;

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

    public List<Statistic> getStatistics(IntervalChoice interval, Weekday weekday) throws SQLException {
        String sql = "SELECT musics_plays.music_id AS music_id, COUNT(*) AS play_count " +
                "FROM musics_plays " +
                "WHERE date > ? AND ('7' = ? OR strftime('%w', date) = ?)" +
                "GROUP BY music_id " +
                "LIMIT " + STATISTICS_LIMIT;

        PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql);

        try {
            stmt.setDate(1, Date.valueOf(interval.getIntervalBegin()));
            stmt.setString(2, Integer.toString(weekday.getWeekdayNumber()));
            stmt.setString(3, Integer.toString(weekday.getWeekdayNumber()));

            ResultSet rs = stmt.executeQuery();

            List<Statistic> result = new ArrayList<>();
            Statistic statistic;

            while (rs.next()) {
                statistic = new Statistic();
                statistic.setMusicId(rs.getInt("music_id"));
                statistic.setPlayCount(rs.getInt("play_count"));
                result.add(statistic);
            }

            return result;
        } finally {
            stmt.close();
        }
    }
}
