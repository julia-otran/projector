package us.guihouse.projector.forms.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.Setter;
import us.guihouse.projector.enums.IntervalChoice;
import us.guihouse.projector.enums.Weekday;
import us.guihouse.projector.models.Statistic;
import us.guihouse.projector.services.ManageMusicService;
import us.guihouse.projector.utils.promise.JavaFxExecutor;
import us.guihouse.projector.utils.promise.Task;

import java.net.URL;
import java.util.List;
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
    private BarChart<String, Integer> barChart;

    @FXML
    private ProgressIndicator loadSpinner;

    @FXML
    private HBox controlBox;

    private List<Statistic> data;

    private XYChart.Series<String, Integer> series;

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

        intervalChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> loadStatistics());


        weekdayChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> loadStatistics());

        this.series = new XYChart.Series<>();
        this.series.setName("Plays X Musicas");
        barChart.getData().addAll(series);
    }

    private void loadStatistics() {
        loading();

        IntervalChoice selectedInterval = intervalChoice.getSelectionModel().getSelectedItem();
        Weekday selectedWeekday = weekdayChoice.getSelectionModel().getSelectedItem();

        manageMusicService.getStatistics(selectedInterval, selectedWeekday).then((Task<List<Statistic>, Void>) (input, callback) -> {
            data = input;
            loadData();
            loaded();
        }, new JavaFxExecutor<>()).execute();
    }

    private void loadData() {
        series.getData().clear();

        for (Statistic statistic : data) {
            series.getData().add(new XYChart.Data<>(statistic.getMusic().getNameWithArtistProperty().getValue(), statistic.getPlayCount()));
        }
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

    public void init() {
        loadStatistics();
    }
}
