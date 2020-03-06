package us.guihouse.projector.forms.controllers;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import us.guihouse.projector.dtos.ImportingMusicDTO;
import us.guihouse.projector.models.Music;
import us.guihouse.projector.models.MusicTheme;
import us.guihouse.projector.services.ManageMusicService;
import us.guihouse.projector.utils.ThemeFinder;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class MusicFormController implements Initializable {
    private ManageMusicService musicService;
    private BackCallback callback;
    private String creatingArtist;
    private Music editingMusic;
    
    @FXML
    private TextField titleTextField;
    
    @FXML
    private ComboBox<String> artistCombo;

    @FXML
    private ComboBox<MusicTheme> themeCombo;
    
    @FXML
    private TextArea musicTextArea;

    private MusicTheme randomTheme = new MusicTheme();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        themeCombo.getItems().clear();
        themeCombo.setConverter(new StringConverter<MusicTheme>() {
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
        themeCombo.setCellFactory(new Callback<ListView<MusicTheme>, ListCell<MusicTheme>>() {
            @Override
            public ListCell<MusicTheme> call(ListView<MusicTheme> param) {
                ListCell<MusicTheme> cell = new ListCell<MusicTheme>() {
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

                return cell;
            }
        });
    }    
    
    @FXML
    public void onCreateArtist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Criar artista");
        dialog.setHeaderText("Incluir um novo artista");
        dialog.setContentText("Digite o nome do artista:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmed = name.trim();
            
            if (trimmed.isEmpty()) {
                creatingArtist = null;
            } else {
                creatingArtist = trimmed;
            }
            
            populateArtists();
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
        String artist = artistCombo.getSelectionModel().getSelectedItem();
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
        ObservableList<String> list = FXCollections.observableArrayList(musicService.listArtists());
        
        if (creatingArtist != null) {
            if (!list.contains(creatingArtist)) {
                list.add(0, creatingArtist);
            }
            artistCombo.setItems(list);
            artistCombo.getSelectionModel().select(creatingArtist);
        } else {
            artistCombo.setItems(list);
        }
    }

    private void populateThemes() {
        themeCombo.getItems().add(randomTheme);
        themeCombo.getItems().addAll(ThemeFinder.getThemes());
    }

    public void init(Integer id) throws ManageMusicService.PersistenceException {
        init();
        editingMusic = musicService.openMusic(id);
        
        if (editingMusic.getArtist() != null) {
            artistCombo.getSelectionModel().select(editingMusic.getArtist().getNameProperty().getValue());
        }
        
        titleTextField.setText(editingMusic.getName());
        musicTextArea.setText(editingMusic.getPhrasesAsString());

        if (editingMusic.getTheme() == null) {
            themeCombo.getSelectionModel().select(randomTheme);
        } else {
            MusicTheme theme = ThemeFinder.getThemeByVideoName(editingMusic.getTheme());
            if (theme != null) {
                themeCombo.getSelectionModel().select(theme);
            } else {
                themeCombo.getSelectionModel().select(randomTheme);
            }
        }
    }

    public void init(ImportingMusicDTO music) {
        creatingArtist = music.getArtist();
        init();
        titleTextField.setText(music.getName());
        musicTextArea.setText(music.getPhrases().stream().collect(Collectors.joining("\n")));
    }
}
