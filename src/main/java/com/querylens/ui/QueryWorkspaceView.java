package com.querylens.ui;

import com.querylens.workspace.QueryExecutionResult;
import com.querylens.workspace.QueryHistoryEntry;
import com.querylens.workspace.facade.QueryWorkspaceService;
import com.querylens.workspace.SavedConnection;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class QueryWorkspaceView extends VBox {
    private final QueryWorkspaceService service;
    private final ComboBox<SavedConnection> connection = new ComboBox<>();
    private final TextArea sql = new TextArea();
    private final Label status = new Label("Choose a saved database connection, then run a query.");
    private final Label analysis = new Label();
    private final Label suggestions = new Label();
    private final TableView<List<String>> rows = new TableView<>();
    private final TableView<QueryHistoryEntry> history = new TableView<>();
    private final Button run = new Button("Run query");

    public QueryWorkspaceView(QueryWorkspaceService service) {
        this.service = service;
        setSpacing(10);
        setPadding(new Insets(16));
        SplitPane resultsPane = createResults();
        getChildren().addAll(createInput(), status, analysis, suggestions, resultsPane);
        VBox.setVgrow(resultsPane, Priority.ALWAYS);
        configureHistory();
        refreshConnections();
        refreshHistory();
    }

    public void refreshConnections() {
        SavedConnection previous = connection.getValue();
        connection.setItems(FXCollections.observableArrayList(service.connections()));
        connection.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(SavedConnection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        });
        connection.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(SavedConnection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choose database" : item.displayName());
            }
        });
        if (previous != null) connection.setValue(previous);
        else if (!connection.getItems().isEmpty()) connection.getSelectionModel().selectFirst();
    }

    private VBox createInput() {
        Label heading = new Label("Query workspace");
        heading.getStyleClass().add("page-title");
        sql.setPrefRowCount(5);
        sql.setWrapText(true);
        sql.setPromptText("Example: SELECT id, customer FROM orders WHERE id = 1");
        HBox.setHgrow(connection, Priority.ALWAYS);
        run.setDefaultButton(true);
        run.setOnAction(event -> execute());
        return new VBox(8, heading, new HBox(8, new Label("Database"), connection, run), new Label("SQL"), sql);
    }

    private SplitPane createResults() {
        rows.setPlaceholder(new Label("Query rows appear here. SELECT results are limited to 100 rows."));
        VBox historyBox = new VBox(6, new Label("Recent query history"), history);
        VBox.setVgrow(history, Priority.ALWAYS);
        SplitPane split = new SplitPane(new VBox(rows), historyBox);
        split.setDividerPositions(0.7);
        return split;
    }

    private void configureHistory() {
        history.getColumns().add(historyColumn("Type", entry -> entry.type().name()));
        history.getColumns().add(historyColumn("Duration", entry -> entry.durationMillis() + " ms"));
        history.getColumns().add(historyColumn("SQL", QueryHistoryEntry::sql));
    }

    private TableColumn<QueryHistoryEntry, String> historyColumn(String title, java.util.function.Function<QueryHistoryEntry, String> value) {
        TableColumn<QueryHistoryEntry, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(title.equals("SQL") ? 340 : 90);
        return column;
    }

    private void execute() {
        SavedConnection selected = connection.getValue();
        if (selected == null) {
            showError("Choose a saved database connection first.");
            return;
        }
        try {
            if (service.requiresMutationConfirmation(sql.getText())) {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                        "This statement changes data in the selected database. Do you want to continue?",
                        ButtonType.OK, ButtonType.CANCEL);
                if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }
        } catch (Exception exception) {
            showError(exception.getMessage());
            return;
        }
        run.setDisable(true);
        Task<QueryExecutionResult> task = new Task<>() {
            @Override
            protected QueryExecutionResult call() {
                return service.run(selected.databasePath(), sql.getText());
            }
        };
        task.setOnSucceeded(event -> {
            showResult(task.getValue());
            run.setDisable(false);
        });
        task.setOnFailed(event -> {
            showError(task.getException().getMessage());
            run.setDisable(false);
        });
        Thread worker = new Thread(task, "querylens-query-workspace");
        worker.setDaemon(true);
        worker.start();
    }

    private void showResult(QueryExecutionResult result) {
        if (result.returnsRows()) {
            configureRows(result.columns(), result.rows());
            status.setText("Returned " + result.rows().size() + " row(s) in " + result.durationMillis() + " ms.");
        } else {
            rows.getColumns().clear();
            rows.getItems().clear();
            status.setText("Changed " + result.affectedRows() + " row(s) in " + result.durationMillis() + " ms.");
        }
        analysis.setText("Analysis: " + result.analysis().queryType() + " • " + result.analysis().riskLevel()
                + " risk • complexity " + result.analysis().complexityScore() + " • tables: " + result.analysis().tables());
        suggestions.setText(result.recommendations().isEmpty()
                ? "No recommendations for this query."
                : "Suggestions: " + String.join("  •  ", result.recommendations().stream().map(item -> item.message()).toList()));
        refreshHistory();
    }

    private void configureRows(List<String> columns, List<List<String>> data) {
        rows.getColumns().clear();
        for (int index = 0; index < columns.size(); index++) {
            int columnIndex = index;
            TableColumn<List<String>, String> column = new TableColumn<>(columns.get(index));
            column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().get(columnIndex)));
            column.setPrefWidth(150);
            rows.getColumns().add(column);
        }
        rows.setItems(FXCollections.observableArrayList(data));
    }

    private void refreshHistory() {
        history.setItems(FXCollections.observableArrayList(service.recentHistory()));
    }

    private void showError(String message) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).showAndWait());
    }
}

