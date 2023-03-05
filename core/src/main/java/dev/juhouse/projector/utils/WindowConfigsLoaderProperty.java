package dev.juhouse.projector.utils;

import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableList;

public interface WindowConfigsLoaderProperty {
    ReadOnlyProperty<String> loadedConfigFileProperty();
    void loadConfigs(String fileName);

    void loadDefaultConfigs();

    ObservableList<String> getConfigFiles();

    boolean createConfigFileFromDefaults(String name);
}
