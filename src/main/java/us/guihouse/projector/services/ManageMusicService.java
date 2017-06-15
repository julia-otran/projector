/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.services;

import us.guihouse.projector.models.Artist;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.other.SQLiteJDBCDriverConnection;
import us.guihouse.projector.repositories.ArtistRepository;
import us.guihouse.projector.repositories.MusicRepository;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author guilherme
 */
public class ManageMusicService {

    public class SaveException extends Exception {
    }

    public class MusicAlreadyPresentException extends SaveException {

        private final Music music;

        public MusicAlreadyPresentException(Music music) {
            this.music = music;
        }

        public Music getMusic() {
            return music;
        }
    }

    public class PersistenceException extends SaveException {

        private final Music music;

        public PersistenceException(Music music) {
            this.music = music;
        }

        public Music getMusic() {
            return music;
        }
    }

    private final ArtistRepository artistRepo = new ArtistRepository();
    private final MusicRepository musicRepo = new MusicRepository();

    public Music createMusic(String name, String artist, String phrases) throws MusicAlreadyPresentException, PersistenceException {
        if (artist != null && artist.isEmpty()) {
            artist = null;
        }

        try {
            SQLiteJDBCDriverConnection.getConn().setAutoCommit(false);
            Artist a = null;

            if (artist != null) {
                try {
                    a = artistRepo.findOrCreateByName(artist);
                } catch (SQLException ex) {
                    Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                    a = null;
                }
            }

            Music m;

            try {
                m = musicRepo.findByNameAndArtist(name, a);

                if (m != null) {
                    throw new MusicAlreadyPresentException(m);
                }

            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                m = null;
            }

            m = new Music();
            m.setArtist(a);
            m.setName(name);
            m.setPhrases(Arrays.asList(phrases.replace("\r\n", "\n").split("\n")));

            try {
                musicRepo.create(m);
                SQLiteJDBCDriverConnection.getConn().commit();
                return m;
            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                throw new PersistenceException(m);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);

            Music m = new Music();
            m.setName(name);
            m.setPhrases(Arrays.asList(phrases.replace("\r\n", "\n").split("\n")));

            throw new PersistenceException(m);
        }
    }

    public Music updateMusic(Integer id, String name, String artist, String phrases) throws MusicAlreadyPresentException, PersistenceException {
        if (artist != null && artist.isEmpty()) {
            artist = null;
        }

        try {
            SQLiteJDBCDriverConnection.getConn().setAutoCommit(false);
            Artist a = null;

            if (artist != null) {
                try {
                    a = artistRepo.findOrCreateByName(artist);
                } catch (SQLException ex) {
                    Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                    a = null;
                }
            }

            Music m;

            try {
                m = musicRepo.findById(id);
            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                m = null;
            }

            if (m == null) {
                m = new Music();
                m.setName(name);
                m.setPhrases(Arrays.asList(phrases.replace("\r\n", "\n").split("\n")));

                throw new PersistenceException(m);
            }

            m.setArtist(a);
            m.setName(name);
            m.setPhrases(Arrays.asList(phrases.replace("\r\n", "\n").split("\n")));

            try {
                musicRepo.update(m);
                SQLiteJDBCDriverConnection.getConn().commit();
                return m;
            } catch (SQLException ex) {
                Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);
                throw new PersistenceException(m);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ManageMusicService.class.getName()).log(Level.SEVERE, null, ex);

            Music m = new Music();
            m.setName(name);
            m.setPhrases(Arrays.asList(phrases.replace("\r\n", "\n").split("\n")));

            throw new PersistenceException(m);
        }
    }

    public List<String> listArtists() {
        return artistRepo.findAll().stream().map(Artist::getName).collect(Collectors.toList());
    }
}
