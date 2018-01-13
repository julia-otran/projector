package us.guihouse.projector.forms.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.Setter;
import us.guihouse.projector.enums.IntervalChoice;
import us.guihouse.projector.enums.Weekday;
import us.guihouse.projector.services.ManageMusicService;

import java.net.URL;
import java.util.Calendar;
import java.util.ResourceBundle;

public class StatisticsController implements Initializable {
    @Setter
    private ManageMusicService manageMusicService;

    @Setter
    private Stage stage;

    @FXML
    private ChoiceBox<IntervalChoice> intervalChoice;

    @FXML
    private ChoiceBox<Weekday> weekdayChoice;

    @FXML
    private BarChart barChart;

    @FXML
    private ProgressIndicator loadSpinner;

    @FXML
    private HBox controlBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        intervalChoice.getItems().addAll(IntervalChoice.values());

        for (IntervalChoice interval : IntervalChoice.values()) {
            if (interval.isSelected()) {
                intervalChoice.getSelectionModel().select(interval);
            }
        }

        weekdayChoice.getItems().addAll(Weekday.values());
        weekdayChoice.getSelectionModel().select(Weekday.ALL);
    }

    private void loading() {
        barChart.setVisible(false);
        controlBox.setVisible(false);
        loadSpinner.setVisible(true);
    }

    private void loaded() {
        barChart.setVisible(true);
        controlBox.setVisible(true);
        loadSpinner.setVisible(false);
    }

}
