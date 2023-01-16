/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.models;

import java.util.Arrays;
import java.util.List;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

/**
 *
 * @author guilherme
 */
@Data
public class Music {

    private final Property<Integer> idProperty;
    private final Property<String> nameProperty;
    private final Property<Artist> artistProperty;
    private final ObservableList<String> phrasesList;
    private final Property<String> themeProperty;

    @Getter(AccessLevel.NONE)
    private final ReadOnlyObjectWrapper<String> nameAndArtistProperty;

    public Music() {
        this.idProperty = new SimpleObjectProperty<>();
        this.nameProperty = new SimpleObjectProperty<>();
        this.artistProperty = new SimpleObjectProperty<>();
        this.nameAndArtistProperty = new ReadOnlyObjectWrapper<>();
        this.phrasesList = FXCollections.observableArrayList();
        this.themeProperty = new SimpleObjectProperty<>();

        setupNameAndArtistProp();
    }

    private void setupNameAndArtistProp() {
        final ChangeListener<String> listener = (observable, oldValue, newValue) -> updateNameAndArtistProp();

        nameProperty.addListener(listener);

        artistProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getNameProperty().removeListener(listener);
            }

            if (newValue != null) {
                newValue.getNameProperty().addListener(listener);
            }

            updateNameAndArtistProp();
        });
    }

    private void updateNameAndArtistProp() {
        String name = nameProperty.getValue();
        String artistName = "";

        if (name == null) {
            name = "";
        }

        if (artistProperty.getValue() != null) {
            artistName = artistProperty.getValue().getNameProperty().getValue();

            if (artistName == null) {
                artistName = "";
            }
        }

        if (name.isEmpty() && artistName.isEmpty()) {
            nameAndArtistProperty.set("");
        } else if (name.isEmpty()) {
            nameAndArtistProperty.set(artistName);
        } else if (artistName.isEmpty()) {
            nameAndArtistProperty.set(name);
        } else {
            nameAndArtistProperty.set(name + " - " + artistName);
        }
    }

    public ReadOnlyProperty<String> getNameWithArtistProperty() {
        return nameAndArtistProperty.getReadOnlyProperty();
    }

    public Integer getId() {
        return idProperty.getValue();
    }

    public void setId(Integer id) {
        idProperty.setValue(id);
    }

    public String getName() {
        return nameProperty.getValue();
    }

    public void setName(String name) {
        nameProperty.setValue(name);
    }

    public Artist getArtist() {
        return artistProperty.getValue();
    }

    public void setArtist(Artist a) {
        artistProperty.setValue(a);
    }

    public void setPhrases(List<String> p) {
        phrasesList.setAll(p);
    }

    public void setPhrases(String text) {
        if (text != null) {
            setPhrases(Arrays.asList(text.replace("\r\n", "\n").split("\n")));
        }
    }

    public String getPhrasesAsString() {
        return String.join("\n", phrasesList);
    }

    public String getTheme() {
        return themeProperty.getValue();
    }

    public void setTheme(String theme) {
        themeProperty.setValue(theme);
    }
}
