package us.guihouse.projector.forms.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
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
import us.guihouse.projector.utils.promise.Callback;
import us.guihouse.projector.utils.promise.JavaFxExecutor;
import us.guihouse.projector.utils.promise.Task;

import java.net.URL;
import java.util.Calendar;
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

        intervalChoice.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<IntervalChoice>() {
            @Override
            public void changed(ObservableValue<? extends IntervalChoice> observable, IntervalChoice oldValue, IntervalChoice newValue) {
                loadStatistics();
            }
        });


        weekdayChoice.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Weekday>() {
            @Override
            public void changed(ObservableValue<? extends Weekday> observable, Weekday oldValue, Weekday newValue) {
                loadStatistics();
            }
        });
    }

    private void loadStatistics() {
        loading();

        IntervalChoice selectedInterval = intervalChoice.getSelectionModel().getSelectedItem();
        Weekday selectedWeekday = weekdayChoice.getSelectionModel().getSelectedItem();

        manageMusicService.getStatistics(selectedInterval, selectedWeekday).then(new Task<List<Statistic>, Void>() {
            @Override
            public void execute(List<Statistic> input, Callback<Void> callback) {
                data = input;
                loadData();
                loaded();
            }
        }, new JavaFxExecutor<>()).execute();
    }

    private void loadData() {
        XYChart.Series series = new XYChart.Series<String, Integer>();
        series.setName("Plays X Musicas");

        for (Statistic statistic : data) {
            series.getData().add(new XYChart.Data<>(statistic.getMusic().getNameWithArtistProperty().getValue(), statistic.getPlayCount()));
        }

        barChart.getData().clear();
        barChart.getData().addAll(series);
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
