/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.services;

import dev.juhouse.projector.repositories.ArtistRepository;
import dev.juhouse.projector.repositories.MetricsRepository;
import dev.juhouse.projector.repositories.MusicRepository;
import dev.juhouse.projector.enums.IntervalChoice;
import dev.juhouse.projector.enums.Weekday;
import dev.juhouse.projector.models.Artist;
import dev.juhouse.projector.models.Music;
import dev.juhouse.projector.models.Statistic;
import dev.juhouse.projector.other.SQLiteJDBCDriverConnection;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import dev.juhouse.projector.dtos.ImportingMusicDTO;
import dev.juhouse.projector.dtos.ListMusicDTO;
import dev.juhouse.projector.utils.promise.BackgroundExecutor;
import dev.juhouse.projector.utils.promise.Promise;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class ManageMusicService {

    public static class SaveException extends Exception {
        public SaveException() { }
        public SaveException(String msg, Throwable cause) {
            super(msg, cause);
        } 
    }

    public static class MusicAlreadyPresentException extends SaveException {

        public MusicAlreadyPresentException(Music music) {
        }

    }

    public static class PersistenceException extends SaveException {
        public PersistenceException(){ }
        
        public PersistenceException(String msg, Throwable cause) {
            super(msg, cause);
        } 
    }
    
    public static class InvalidData extends SaveException { }
    public static class InavlidArtist extends InvalidData { }
    public static class InvalidName extends InvalidData { }
    public static class InvalidPhrases extends InvalidData { }

    private final ArtistRepository artistRepo = new ArtistRepository();
    private final MusicRepository musicRepo = new MusicRepository();
    private final MetricsRepository metricRepo = new MetricsRepository();
    private final Map<Music, Integer> openedMusics = new HashMap<>();
    
    /**
     * Keeps the musics in cache so properties events works correcly
     * @param id music id
     * @return the music, or null
     * @throws ManageMusicService.PersistenceException Only for DB errors
     */
    public Music openMusic(Integer id) throws PersistenceException {
        Optional<Entry<Music, Integer>> cache = openedMusics.entrySet().stream()
                .filter(m -> Objects.equals(m.getKey().getId(), id))
                .findAny();
        
        if (cache.isPresent()) {
            Entry<Music, Integer> e = cache.get();
            e.setValue(e.getValue() + 1);
            return e.getKey();
        }
        
        try {
            Music m = musicRepo.findById(id);
            
            if (m != null) {
                openedMusics.put(m, 1);
            }
            
            return m;
        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }
    
    /**
     * Same as #openMusic, but registers the play in metrics.
     * @param id music id
     * @return the music, or null
     * @throws ManageMusicService.PersistenceException Only for DB errors
     */
    public Music openMusicForPlay(Integer id) throws PersistenceException {
        Music m = openMusic(id);
        
        if (m != null) {
            try {
                metricRepo.registerPlay(m);
            } catch (SQLException ex) {
                // Ops, this is only metrics, so go ahead.
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return m;
    }
    
    public void closeMusic(Integer id) {
        Optional<Entry<Music, Integer>> cache = openedMusics.entrySet().stream()
                .filter(m -> Objects.equals(m.getKey().getId(), id))
                .findAny();
        
        if (cache.isPresent()) {
            Entry<Music, Integer> e = cache.get();
            
            int newCount = e.getValue() - 1;
            
            if (newCount > 0) {
                e.setValue(newCount);
            } else {
                openedMusics.remove(e.getKey());
            }
        }
    }
    
    private Artist findOrCrateArtist(String name) throws PersistenceException, InavlidArtist {
        if (name == null || name.trim().isEmpty()) {
            throw new InavlidArtist();
        }
        
        try {
            return artistRepo.findOrCreateByName(name);
        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
            
            try {
                SQLiteJDBCDriverConnection.getConn().rollback();
            } catch (SQLException ex1) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex1);
            }
            
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }
    
    public boolean alreadyExists(ImportingMusicDTO music) throws SQLException {
        Artist a = artistRepo.findByName(music.getArtist());
        
        if (a == null) {
            return false;
        }
        
        Music m = musicRepo.findByNameAndArtist(music.getName(), a);

        return m != null;
    }

    public Integer createMusic(String name, String artist, String phrases, String theme) throws MusicAlreadyPresentException, PersistenceException, InavlidArtist, InvalidName, InvalidPhrases {
        try {
            name = name.trim();
            phrases = phrases.trim();
            
            if (name.isEmpty()) {
                throw new InvalidName();
            }
            
            if (phrases.isEmpty()) {
                throw new InvalidPhrases();
            }
            
            if (!phrases.endsWith("\n")) {
                phrases = phrases + "\n";
            }
            
            SQLiteJDBCDriverConnection.getConn().setAutoCommit(false);
            
            Artist a = findOrCrateArtist(artist);
            Music m;

            try {
                m = musicRepo.findByNameAndArtist(name, a);

                if (m != null) {
                    SQLiteJDBCDriverConnection.getConn().rollback();
                    SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                    throw new MusicAlreadyPresentException(m);
                }

            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                SQLiteJDBCDriverConnection.getConn().rollback();
                SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                throw new PersistenceException(ex.getMessage(), ex);
            }

            m = new Music();
            m.setArtist(a);
            m.setName(name);
            m.setPhrases(phrases);
            m.setTheme(theme);

            try {
                musicRepo.create(m);
                SQLiteJDBCDriverConnection.getConn().commit();
                SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                return m.getId();
            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                SQLiteJDBCDriverConnection.getConn().rollback();
                SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                throw new PersistenceException(ex.getMessage(), ex);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }

    public void updateMusic(Integer id, String name, String artist, String phrases, String theme) throws PersistenceException, InavlidArtist {
        try {
            SQLiteJDBCDriverConnection.getConn().setAutoCommit(false);
            Artist a = findOrCrateArtist(artist);

            Music m;

            try {
                m = openMusic(id);
                
                if (m == null) {
                    SQLiteJDBCDriverConnection.getConn().rollback();
                    SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                    throw new PersistenceException();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                SQLiteJDBCDriverConnection.getConn().rollback();
                SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                throw new PersistenceException(ex.getMessage(), ex);
            }

            m.setArtist(a);
            m.setName(name);
            m.setPhrases(phrases);
            m.setTheme(theme);

            try {
                musicRepo.update(m);
                SQLiteJDBCDriverConnection.getConn().commit();
                SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                closeMusic(id);
            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                SQLiteJDBCDriverConnection.getConn().rollback();
                SQLiteJDBCDriverConnection.getConn().setAutoCommit(true);
                throw new PersistenceException(ex.getMessage(), ex);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
            throw new PersistenceException(ex.getMessage(), ex);
        }
    }

    public List<String> searchArtists(String term) {
        return artistRepo.search(term).stream()
                .map(a -> a.getNameProperty().getValue())
                .collect(Collectors.toList());
    }
    
    public List<ListMusicDTO> listByTermIfPresentWithLimit(String term) throws PersistenceException {
        List<ListMusicDTO> musics;
        
        try {
            if (term == null || term.isEmpty()) {
                musics = musicRepo.listWithLimit();
            } else {
                musics = musicRepo.listByTermWithLimit(term);
            }
            
            return musics;
        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
            throw new PersistenceException(ex.getMessage(), ex);
        }           
    }

    public Promise<List<Statistic>> getStatistics(IntervalChoice interval, Weekday weekday) {
        return Promise.create((input, callback) -> {
            try {

                List<Statistic> statistics = metricRepo.getStatistics(interval, weekday);

                statistics.forEach(s -> {
                    try {
                        s.setMusic(musicRepo.findById(s.getMusicId()));
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });

                callback.success(statistics);
            } catch (SQLException e) {
                e.printStackTrace();
                callback.error(e);
            }
        }, new BackgroundExecutor<>());

    }
}
