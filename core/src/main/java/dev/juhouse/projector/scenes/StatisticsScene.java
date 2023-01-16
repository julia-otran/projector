package dev.juhouse.projector.scenes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import dev.juhouse.projector.forms.controllers.StatisticsController;
import dev.juhouse.projector.services.ManageMusicService;

import java.io.IOException;
import java.net.URL;

public class StatisticsScene {
    public static Parent createStatisticsScene(ManageMusicService manageMusicService, Stage stage) throws IOException {
        URL url = StatisticsScene.class.getClassLoader().getResource("fxml/statistics.fxml");
        FXMLLoader loader = new FXMLLoader(url);

        Parent root = loader.load();
        StatisticsController ctrl = loader.getController();
        ctrl.setManageMusicService(manageMusicService);
        ctrl.setStage(stage);
        ctrl.init();

        return root;
    }
}
