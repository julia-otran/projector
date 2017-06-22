/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.models;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Data;

/**
 *
 * @author guilherme
 */
@Data
public class Artist {
    private final Property<Integer> idProperty;
    private final Property<String> nameProperty;

    public Artist(Artist artist) {
        this.idProperty = new SimpleObjectProperty<>(artist.getIdProperty().getValue());
        this.nameProperty = new SimpleObjectProperty<>(artist.getNameProperty().getValue());
    }

    public Artist() {
        this.idProperty = new SimpleObjectProperty<>();
        this.nameProperty = new SimpleObjectProperty<>();
    }
}
