package com.querylens.ui.benchmark;

import com.querylens.benchmark.model.BenchmarkSettings;
import com.querylens.benchmark.model.RankingStrategy;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.Duration;

public final class BenchmarkControlsView extends GridPane {
    private final Spinner<Integer> warmups = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 1));
    private final Spinner<Integer> measuredRuns = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5));
    private final Spinner<Integer> timeoutSeconds = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 10));
    private final Spinner<Integer> candidates = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 10));
    private final ComboBox<RankingStrategy> ranking = new ComboBox<>();

    public BenchmarkControlsView() {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(16, 0, 0, 0));
        ranking.getItems().setAll(RankingStrategy.values());
        ranking.setValue(RankingStrategy.MEDIAN);

        addRow(0, new Label("Warm-up runs"), warmups);
        addRow(1, new Label("Measured runs"), measuredRuns);
        addRow(2, new Label("Timeout (seconds)"), timeoutSeconds);
        addRow(3, new Label("Maximum candidates"), candidates);
        addRow(4, new Label("Rank by"), ranking);

        Button cancel = new Button("Cancel benchmark");
        cancel.setDisable(true);
        add(new HBox(10, cancel), 1, 5);
    }

    public BenchmarkSettings settings() {
        return new BenchmarkSettings(warmups.getValue(), measuredRuns.getValue(),
                Duration.ofSeconds(timeoutSeconds.getValue()), candidates.getValue(), ranking.getValue());
    }
}
