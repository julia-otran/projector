package dev.juhouse.projector.forms.controllers;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.juhouse.projector.dtos.ImportingMusicDTO;
import dev.juhouse.projector.models.Artist;
import dev.juhouse.projector.models.Music;
import dev.juhouse.projector.models.MusicTheme;
import dev.juhouse.projector.services.ManageMusicService;
import dev.juhouse.projector.utils.ThemeFinder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

/**
 * FXML Controller class
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class MusicFormController implements Initializable {
    private ManageMusicService musicService;
    private BackCallback callback;
    private Music editingMusic;
    
    @FXML
    private TextField titleTextField;
    
    @FXML
    private TextField artistTextField;

    @FXML
    private ComboBox<MusicTheme> themeCombo;
    
    @FXML
    private TextArea musicTextArea;

    private final MusicTheme randomTheme = new MusicTheme();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        themeCombo.getItems().clear();
        themeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MusicTheme object) {
                if (object.equals(randomTheme)) {
                    return "Aleatório";
                } else {
                    return object.getVideoFile().getName();
                }
            }

            @Override
            public MusicTheme fromString(String string) {
                if ("Aleatório".equals(string)) {
                    return randomTheme;
                }

                return ThemeFinder.getThemeByVideoName(string);
            }
        });
        themeCombo.setCellFactory(new Callback<>() {
            @Override
            public ListCell<MusicTheme> call(ListView<MusicTheme> param) {

                return new ListCell<>() {
                    @Override
                    protected void updateItem(MusicTheme item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else if (item.equals(randomTheme)) {
                            setText("Aleatório");
                            setGraphic(null);
                        } else {
                            setGraphic(item.getImage());
                            setText(null);
                        }
                    }
                };
            }
        });
    }    

    @FXML
    public void onCancel() {
        if (editingMusic != null) {
            musicService.closeMusic(editingMusic.getId());
        }
        
        getCallback().goBack();
    }
    
    @FXML
    public void onSaveAndAdd() {
        save(true);
    }
    
    @FXML
    public void onSave() {
        save(false);
    }
    
    private void save(boolean addToList) {
        String name = titleTextField.getText();
        String artist = artistTextField.getText();
        String music = musicTextArea.getText();

        MusicTheme theme = themeCombo.getSelectionModel().getSelectedItem();

        String themeName;

        if (theme.equals(randomTheme)) {
            themeName = null;
        } else {
            themeName = theme.getVideoFile().getName();
        }
        
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText("Falha ao salvar música.");
        
        try {
            if (editingMusic == null) {
                Integer createdId = musicService.createMusic(name, artist, music, themeName);
                if (addToList) {
                    getCallback().goBackWithId(createdId);
                } else {
                    getCallback().goBackAndReload();
                }
            } else {
                musicService.updateMusic(editingMusic.getId(), name, artist, music, themeName);
                musicService.closeMusic(editingMusic.getId());
                
                if (addToList) {
                    getCallback().goBackWithId(editingMusic.getId());
                } else {
                    getCallback().goBackAndReload();
                }
            }
            
        } catch (ManageMusicService.MusicAlreadyPresentException ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Já existe uma música com mesmo nome e artista.");
            alert.showAndWait();
        } catch (ManageMusicService.PersistenceException ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Erro desconhecido de persistência. Verifique a base de dados.");
            alert.showAndWait();
        } catch (ManageMusicService.InavlidArtist ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Verifique o nome do artista.");
            alert.showAndWait();
        } catch (ManageMusicService.InvalidName ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Verifique o título da música.");
            alert.showAndWait();
        } catch (ManageMusicService.InvalidPhrases ex) {
            Logger.getLogger(MusicFormController.class.getName()).log(Level.SEVERE, null, ex);
            alert.setContentText("Verifique a letra.");
            alert.showAndWait();
        }
    }

    public void setManageMusicService(ManageMusicService manageMusicService) {
        this.musicService = manageMusicService;
    }

    public void setBackCallback(BackCallback callback) {
        this.callback = callback;
    }

    public BackCallback getCallback() {
        return callback;
    }

    public void init() {
        populateArtists();
        populateThemes();
        themeCombo.getSelectionModel().select(randomTheme);
    }

    private void populateArtists() {
        TextFields.bindAutoCompletion(artistTextField, suggestionRequest -> musicService.searchArtists(suggestionRequest.getUserText()));
    }

    private void populateThemes() {
        themeCombo.getItems().add(randomTheme);
        themeCombo.getItems().addAll(ThemeFinder.getThemes());
    }

    public void init(Integer id) throws ManageMusicService.PersistenceException {
        init();
        editingMusic = musicService.openMusic(id);
        
        if (editingMusic.getArtist() != null) {
            artistTextField.setText(editingMusic.getArtist().getNameProperty().getValue());
        }
        
        titleTextField.setText(editingMusic.getName());
        musicTextArea.setText(editingMusic.getPhrasesAsString());

        if (editingMusic.getTheme() == null) {
            themeCombo.getSelectionModel().select(randomTheme);
        } else {
            MusicTheme theme = ThemeFinder.getThemeByVideoName(editingMusic.getTheme());
            themeCombo.getSelectionModel().select(Objects.requireNonNullElse(theme, randomTheme));
        }
    }

    public void init(ImportingMusicDTO music) {
        init();
        titleTextField.setText(music.getName());
        artistTextField.setText(music.getArtist());
        musicTextArea.setText(String.join("\n", music.getPhrases()));
    }
}
