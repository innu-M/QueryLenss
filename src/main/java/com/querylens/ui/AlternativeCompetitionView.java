package com.querylens.ui;

import com.querylens.alternative.AlternativeCompetitionResult;
import com.querylens.alternative.AlternativeQueryCompetitionService;
import com.querylens.alternative.CompetitionCandidate;
import com.querylens.benchmark.BenchmarkProgress;
import com.querylens.benchmark.BenchmarkSettings;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AlternativeCompetitionView extends BorderPane {
    private final AlternativeQueryCompetitionService competitionService;
    private final TextField databasePath = new TextField();
    private final TextArea sql = new TextArea();
    private final BenchmarkControlsView benchmarkControls = new BenchmarkControlsView();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label progressText = new Label("Ready.");
    private final Label winner = new Label("Run a competition to find the best verified candidate.");
    private final TableView<CompetitionCandidate> results = new TableView<>();
    private final TextArea details = new TextArea();
    private final Button run = new Button("Generate and benchmark alternatives");
    private final Button cancel = new Button("Cancel");

    public AlternativeCompetitionView(AlternativeQueryCompetitionService competitionService) {
        this.competitionService = competitionService;
        setPadding(new Insets(16));
        setTop(createInputPanel());
        setCenter(createResultsPanel());
        configureResultsTable();
    }

    private VBox createInputPanel() {
        Label heading = new Label("Alternative SQL Competition");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label note = new Label(
                "For safe single-table SELECT queries, QueryLens generates candidates, verifies results, benchmarks them, and saves the report.");

        databasePath.setPromptText("Path to the SQLite database being analysed");
        HBox.setHgrow(databasePath, Priority.ALWAYS);
        Button browse = new Button("Browse…");
        browse.setOnAction(event -> chooseDatabase());
        HBox databaseRow = new HBox(8, new Label("Database"), databasePath, browse);

        sql.setPromptText("Example: SELECT name FROM sailors WHERE rating = 8 OR rating = 10");
        sql.setPrefRowCount(4);
        sql.setWrapText(true);

        run.setDefaultButton(true);
        run.setOnAction(event -> runCompetition());
        cancel.setDisable(true);
        cancel.setOnAction(event -> {
            competitionService.cancel();
            progressText.setText("Cancellation requested…");
        });
        progress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progress, Priority.ALWAYS);
        HBox actions = new HBox(8, run, cancel, progress, progressText);

        return new VBox(8, heading, note, databaseRow, new Label("SQL"), sql,
                new Label("Benchmark settings"), benchmarkControls, actions);
    }

    private SplitPane createResultsPanel() {
        winner.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        VBox tablePanel = new VBox(8, winner, results);
        VBox.setVgrow(results, Priority.ALWAYS);

        details.setEditable(false);
        details.setWrapText(true);
        details.setPromptText("Select a candidate to see its SQL, explanation, plan, and measured samples.");
        VBox detailsPanel = new VBox(8, new Label("Candidate evidence"), details);
        VBox.setVgrow(details, Priority.ALWAYS);

        SplitPane split = new SplitPane(tablePanel, detailsPanel);
        split.setDividerPositions(0.58);
        BorderPane.setMargin(split, new Insets(14, 0, 0, 0));
        return split;
    }

    private void configureResultsTable() {
        results.setPlaceholder(new Label("No benchmark results yet."));
        results.getColumns().add(column("Rank", 55, candidate -> candidate.rank() == 0 ? "—" : candidate.rank()));
        results.getColumns().add(column("Candidate", 170, CompetitionCandidate::label));
        results.getColumns().add(column("Median", 95, candidate -> formatNanos(candidate.medianNs())));
        results.getColumns().add(column("P95", 95, candidate -> formatNanos(candidate.p95Ns())));
        results.getColumns().add(column("Beats", 80, candidate -> String.format("%.0f%%", candidate.beatsPercent())));
        results.getColumns().add(column("Equivalent", 85, candidate -> candidate.equivalent() ? "Yes" : "No"));
        results.getColumns().add(column("Status", 125, CompetitionCandidate::status));
        results.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showDetails(selected));
    }

    private <T> TableColumn<CompetitionCandidate, T> column(
            String title, double width, Function<CompetitionCandidate, T> value) {
        TableColumn<CompetitionCandidate, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return column;
    }

    private void chooseDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose SQLite database");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQLite databases", "*.db", "*.sqlite", "*.sqlite3"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) databasePath.setText(selected.getAbsolutePath());
    }

    private void runCompetition() {
        Path selectedDatabase;
        try {
            selectedDatabase = Path.of(databasePath.getText().strip());
        } catch (RuntimeException exception) {
            showError("Invalid database path", "The selected file path is not valid.");
            return;
        }
        String submittedSql = sql.getText();
        BenchmarkSettings settings = benchmarkControls.settings();
        setRunning(true);
        results.getItems().clear();
        details.clear();
        winner.setText("Generating and measuring candidates…");

        Task<AlternativeCompetitionResult> task = new Task<>() {
            @Override
            protected AlternativeCompetitionResult call() {
                return competitionService.compete(selectedDatabase, submittedSql, settings,
                        benchmarkProgress -> Platform.runLater(
                                () -> AlternativeCompetitionView.this.updateProgress(benchmarkProgress)));
            }
        };
        task.setOnSucceeded(event -> {
            showResult(task.getValue());
            setRunning(false);
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            showError("Competition failed", failure == null ? "Unknown error." : failure.getMessage());
            winner.setText("No winner was selected.");
            setRunning(false);
        });
        Thread worker = new Thread(task, "querylens-alternative-competition");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateProgress(BenchmarkProgress benchmarkProgress) {
        int total = Math.max(1, benchmarkProgress.totalRuns());
        progress.setProgress((double) benchmarkProgress.completedRuns() / total);
        progressText.setText("%s: %s %d/%d".formatted(
                benchmarkProgress.candidateLabel(), benchmarkProgress.status(),
                benchmarkProgress.completedRuns(), benchmarkProgress.totalRuns()));
    }

    private void showResult(AlternativeCompetitionResult competition) {
        var ordered = competition.candidates().stream()
                .sorted(Comparator.comparingInt(candidate -> candidate.rank() == 0 ? Integer.MAX_VALUE : candidate.rank()))
                .toList();
        results.setItems(FXCollections.observableArrayList(ordered));
        CompetitionCandidate winningCandidate = competition.winner();
        winner.setText(winningCandidate == null
                ? "No equivalent candidate completed enough measurements to rank."
                : "%s wins • %s median • Beats %.0f%% • saved as comparison #%d".formatted(
                winningCandidate.label(), formatNanos(winningCandidate.medianNs()),
                winningCandidate.beatsPercent(), competition.comparisonId()));
        progress.setProgress(1);
        progressText.setText("Competition completed and saved to history.");
        if (!ordered.isEmpty()) results.getSelectionModel().selectFirst();
    }

    private void showDetails(CompetitionCandidate candidate) {
        if (candidate == null) {
            details.clear();
            return;
        }
        String samples = candidate.samplesNs().stream().map(this::formatNanos).collect(Collectors.joining(", "));
        details.setText("""
                Rank: %s
                Result equivalence: %s
                Status: %s
                Local percentile: Beats %.1f%% of verified candidates
                Samples: %s

                Why
                %s

                SQL
                %s

                EXPLAIN QUERY PLAN
                %s
                """.formatted(candidate.rank() == 0 ? "Not ranked" : candidate.rank(),
                candidate.equivalent() ? "Verified" : "Rejected", candidate.status(), candidate.beatsPercent(),
                samples.isBlank() ? "No completed samples" : samples, candidate.explanation(),
                candidate.sql(), candidate.planText()));
    }

    private void setRunning(boolean running) {
        run.setDisable(running);
        cancel.setDisable(!running);
        databasePath.setDisable(running);
        sql.setDisable(running);
        if (running) progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
    }

    private String formatNanos(long nanoseconds) {
        if (nanoseconds <= 0) return "—";
        if (nanoseconds >= 1_000_000) return String.format("%.2f ms", nanoseconds / 1_000_000.0);
        if (nanoseconds >= 1_000) return String.format("%.2f μs", nanoseconds / 1_000.0);
        return nanoseconds + " ns";
    }

    private void showError(String heading, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(heading);
        alert.showAndWait();
        progressText.setText(message);
    }
}
