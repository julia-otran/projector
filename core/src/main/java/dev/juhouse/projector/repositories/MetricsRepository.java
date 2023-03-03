/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.repositories;

import dev.juhouse.projector.enums.IntervalChoice;
import dev.juhouse.projector.enums.Weekday;
import dev.juhouse.projector.models.Music;
import dev.juhouse.projector.models.Statistic;
import dev.juhouse.projector.other.SQLiteJDBCDriverConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class MetricsRepository {
    private static final int STATISTICS_LIMIT = 15;

    public void registerPlay(Music music) throws SQLException {
        LocalDate now = LocalDate.now(ZoneOffset.UTC.normalized());
        String sql = "INSERT INTO musics_plays(music_id, date) VALUES (?, ?)";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setInt(1, music.getId());
            stmt.setDate(2, Date.valueOf(now));

            stmt.execute();
        }
    }

    public List<Statistic> getStatistics(IntervalChoice interval, Weekday weekday) throws SQLException {
        Map<Integer, Statistic> statistics = new HashMap<>();

        String sql = "SELECT musics_plays.music_id AS music_id, musics_plays.date as date " +
                "FROM musics_plays " +
                "WHERE date > ? ";

        try (PreparedStatement stmt = SQLiteJDBCDriverConnection
                .getConn()
                .prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(interval.getIntervalBegin()));

            ResultSet rs = stmt.executeQuery();

            Statistic statistic;
            Integer musicId;
            Date date;

            while (rs.next()) {
                date = rs.getDate("date");

                if (weekday.isWeekday(date)) {
                    musicId = rs.getInt("music_id");
                    statistic = statistics.get(musicId);

                    if (statistic == null) {
                        statistic = new Statistic();
                        statistic.setMusicId(musicId);
                        statistic.setPlayCount(1);
                        statistics.put(musicId, statistic);
                    } else {
                        statistic.setPlayCount(statistic.getPlayCount() + 1);
                    }
                }
            }

            return statistics.values()
                    .stream()
                    .sorted((s1, s2) -> Integer.compare(s2.getPlayCount(), s1.getPlayCount()))
                    .limit(STATISTICS_LIMIT)
                    .collect(Collectors.toList());
        }
    }
}
