package com.querylens.ui.recommendation;

import com.querylens.recommendation.model.Recommendation;
import com.querylens.recommendation.model.RecommendationStatus;
import com.querylens.workspace.facade.QueryWorkspaceService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class RecommendationsView extends VBox {
    private final QueryWorkspaceService service;
    private final TableView<Recommendation> recommendations = new TableView<>();
    private final Label status = new Label("Run a query to receive suggestions.");
    private final Button apply = new Button("Mark applied");
    private final Button dismiss = new Button("Dismiss");

    public RecommendationsView(QueryWorkspaceService service) {
        this.service = service;
        setSpacing(10);
        setPadding(new Insets(16));
        configureTable();
        apply.setOnAction(event -> updateSelected(true));
        dismiss.setOnAction(event -> updateSelected(false));
        getChildren().addAll(new Label("Recommendations"), status, recommendations, new HBox(8, apply, dismiss));
        VBox.setVgrow(recommendations, Priority.ALWAYS);
        refresh();
    }

    public void refresh() {
        recommendations.setItems(FXCollections.observableArrayList(service.recommendations()));
        updateButtons();
    }

    private void configureTable() {
        recommendations.getColumns().add(column("Suggestion", Recommendation::message, 650));
        recommendations.getColumns().add(column("Status", item -> item.status().name(), 130));
        recommendations.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> updateButtons());
        recommendations.setPlaceholder(new Label("No recommendations yet. Run a query from Query Workspace."));
    }

    private TableColumn<Recommendation, String> column(String title,
                                                         java.util.function.Function<Recommendation, String> value,
                                                         double width) {
        TableColumn<Recommendation, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void updateSelected(boolean markApplied) {
        Recommendation selected = recommendations.getSelectionModel().getSelectedItem();
        if (selected == null || selected.status() != RecommendationStatus.PENDING) return;
        try {
            if (markApplied) service.applyRecommendation(selected.id());
            else service.dismissRecommendation(selected.id());
            status.setText(markApplied ? "Recommendation marked as applied." : "Recommendation dismissed.");
            refresh();
        } catch (Exception exception) {
            new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
        }
    }

    private void updateButtons() {
        Recommendation selected = recommendations.getSelectionModel().getSelectedItem();
        boolean pending = selected != null && selected.status() == RecommendationStatus.PENDING;
        apply.setDisable(!pending);
        dismiss.setDisable(!pending);
    }
}

