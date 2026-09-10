package com.querylens.ui.history;

import com.querylens.history.model.ComparisonCandidateEntry;
import com.querylens.history.model.ComparisonHistorySummary;
import com.querylens.history.model.ComparisonSessionEntry;
import com.querylens.persistence.comparison.ComparisonHistoryRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ComparisonHistoryView extends BorderPane {
    private final ComparisonHistoryRepository repository;
    private final TextField search = new TextField();
    private final Label summary = new Label();
    private final Label status = new Label();
    private final TableView<ComparisonSessionEntry> sessions = new TableView<>();
    private final TableView<ComparisonCandidateEntry> candidates = new TableView<>();
    private final TextArea details = new TextArea();
    private final Button delete = new Button("Delete selected");

    public ComparisonHistoryView(ComparisonHistoryRepository repository) {
        this.repository = repository;
        setPadding(new Insets(16));
        setTop(createHeader());
        setCenter(createContent());
        setBottom(status);
        BorderPane.setMargin(status, new Insets(10, 0, 0, 0));
        configureTables();
        refresh();
    }

    private VBox createHeader() {
        Label heading = new Label("Comparison History & Reports");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        search.setPromptText("Search SQL or database path");
        search.setOnAction(event -> refresh());
        HBox.setHgrow(search, Priority.ALWAYS);

        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> refresh());
        Button clear = new Button("Clear");
        clear.setOnAction(event -> {
            search.clear();
            refresh();
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        delete.setDisable(true);
        delete.setOnAction(event -> deleteSelected());

        return new VBox(10, heading, summary,
                new HBox(8, search, searchButton, clear, refresh, delete));
    }

    private SplitPane createContent() {
        VBox sessionPanel = new VBox(8, new Label("Saved comparisons"), sessions);
        VBox.setVgrow(sessions, Priority.ALWAYS);

        details.setEditable(false);
        details.setWrapText(true);
        details.setPromptText("Select a candidate to inspect its SQL, plan, explanation, and benchmark samples.");
        VBox candidatePanel = new VBox(8, new Label("Ranked candidates"), candidates,
                new Label("Candidate report"), details);
        VBox.setVgrow(candidates, Priority.ALWAYS);
        VBox.setVgrow(details, Priority.ALWAYS);

        SplitPane split = new SplitPane(sessionPanel, candidatePanel);
        split.setDividerPositions(0.48);
        BorderPane.setMargin(split, new Insets(14, 0, 0, 0));
        return split;
    }

    private void configureTables() {
        sessions.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        sessions.setPlaceholder(new Label("No saved comparisons match this search."));
        sessions.getColumns().add(column("SQL", 260, ComparisonSessionEntry::originalSql));
        sessions.getColumns().add(column("Winner", 130, ComparisonSessionEntry::winnerLabel));
        sessions.getColumns().add(column("Candidates", 85, entry -> entry.candidateCount()));
        sessions.getColumns().add(column("Improvement", 105,
                entry -> String.format("%.1f%%", entry.improvementPercent())));
        sessions.getColumns().add(column("Created", 145, ComparisonSessionEntry::createdAt));
        sessions.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> loadCandidates(selected));

        candidates.setPlaceholder(new Label("Select a saved comparison to see its candidates."));
        candidates.getColumns().add(column("Rank", 55, entry -> entry.rank() == 0 ? "—" : entry.rank()));
        candidates.getColumns().add(column("Candidate", 150, ComparisonCandidateEntry::label));
        candidates.getColumns().add(column("Median", 95, entry -> formatNanos(entry.medianNs())));
        candidates.getColumns().add(column("P95", 95, entry -> formatNanos(entry.p95Ns())));
        candidates.getColumns().add(column("Verified", 75, entry -> entry.equivalent() ? "Yes" : "No"));
        candidates.getColumns().add(column("Runs", 55, ComparisonCandidateEntry::sampleCount));
        candidates.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showCandidateDetails(selected));
    }

    private <S, T> TableColumn<S, T> column(String title, double width,
                                             java.util.function.Function<S, T> value) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return column;
    }

    private void refresh() {
        try {
            sessions.setItems(FXCollections.observableArrayList(repository.findSessions(search.getText())));
            candidates.getItems().clear();
            details.clear();
            delete.setDisable(true);
            updateSummary();
            status.setText(sessions.getItems().size() + " comparison(s) shown.");
        } catch (IllegalStateException exception) {
            showError("Could not load history", exception.getMessage());
        }
    }

    private void updateSummary() {
        ComparisonHistorySummary data = repository.summarize();
        summary.setText(String.format(
                "Sessions: %d   •   Verified candidates: %d   •   Average improvement: %.1f%%   •   Best: %.1f%%",
                data.totalSessions(), data.verifiedCandidates(),
                data.averageImprovementPercent(), data.bestImprovementPercent()));
    }

    private void loadCandidates(ComparisonSessionEntry selected) {
        candidates.getItems().clear();
        details.clear();
        delete.setDisable(selected == null);
        if (selected == null) return;
        try {
            candidates.setItems(FXCollections.observableArrayList(repository.findCandidates(selected.id())));
            if (!candidates.getItems().isEmpty()) candidates.getSelectionModel().selectFirst();
        } catch (IllegalStateException exception) {
            showError("Could not load candidates", exception.getMessage());
        }
    }

    private void showCandidateDetails(ComparisonCandidateEntry candidate) {
        if (candidate == null) {
            details.clear();
            return;
        }
        List<Long> samples = repository.findRunDurations(candidate.id());
        String runText = samples.stream().map(this::formatNanos).collect(Collectors.joining(", "));
        details.setText("""
                Status: %s
                Equivalent result: %s
                Benchmark samples: %s

                Why this candidate differs
                %s

                SQL
                %s

                EXPLAIN QUERY PLAN
                %s
                """.formatted(candidate.status(), candidate.equivalent() ? "Verified" : "Not verified",
                runText, candidate.explanation(), candidate.sql(), candidate.planText()));
    }

    private void deleteSelected() {
        ComparisonSessionEntry selected = sessions.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this comparison and all of its candidate benchmark runs?",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("Delete saved comparison");
        Optional<ButtonType> response = confirmation.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            repository.delete(selected.id());
            refresh();
            status.setText("Comparison deleted.");
        }
    }

    private String formatNanos(long nanoseconds) {
        if (nanoseconds >= 1_000_000) return String.format("%.2f ms", nanoseconds / 1_000_000.0);
        if (nanoseconds >= 1_000) return String.format("%.2f μs", nanoseconds / 1_000.0);
        return nanoseconds + " ns";
    }

    private void showError(String heading, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(heading);
        alert.showAndWait();
        status.setText(message);
    }
}
