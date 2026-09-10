package com.querylens.ui.query;

import com.querylens.workspace.facade.QueryWorkspaceService;
import com.querylens.workspace.model.SavedQuery;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Consumer;

/** Lets users maintain a small library of reusable SQL statements. */
public final class SavedQueriesView extends VBox {
    private final QueryWorkspaceService service;
    private final Consumer<String> onLoad;
    private final TextField title = new TextField();
    private final TextArea sql = new TextArea();
    private final TableView<SavedQuery> queries = new TableView<>();
    private final Button update = new Button("Update selected");
    private final Button delete = new Button("Delete selected");

    public SavedQueriesView(QueryWorkspaceService service, Consumer<String> onLoad) {
        this.service = service;
        this.onLoad = onLoad;
        setSpacing(10);
        setPadding(new Insets(20));
        configureTable();
        getChildren().addAll(createForm(), queries);
        VBox.setVgrow(queries, Priority.ALWAYS);
        refresh();
    }

    private VBox createForm() {
        Label heading = new Label("Saved queries");
        heading.getStyleClass().add("page-title");
        title.setPromptText("A helpful name, for example Recent customer orders");
        sql.setPromptText("SELECT customer_id, status FROM orders ORDER BY customer_id");
        sql.setPrefRowCount(5);
        sql.setWrapText(true);
        Button save = new Button("Save query");
        save.setOnAction(event -> save());
        update.setDisable(true);
        update.setOnAction(event -> update());
        delete.setDisable(true);
        delete.setOnAction(event -> delete());
        Button load = new Button("Load in workspace");
        load.setOnAction(event -> loadSelected());
        Button clear = new Button("Clear selection");
        clear.setOnAction(event -> clearSelection());
        return new VBox(8, heading, new Label("Title"), title, new Label("SQL"), sql,
                new HBox(8, save, update, delete, load, clear));
    }

    private void configureTable() {
        queries.setPlaceholder(new Label("Save a query here to build your reusable SQL library."));
        queries.getColumns().add(column("Title", SavedQuery::title, 200));
        queries.getColumns().add(column("SQL", SavedQuery::sql, 460));
        queries.getColumns().add(column("Last updated", SavedQuery::updatedAt, 160));
        queries.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            update.setDisable(selected == null);
            delete.setDisable(selected == null);
            if (selected != null) {
                title.setText(selected.title());
                sql.setText(selected.sql());
            }
        });
    }

    private TableColumn<SavedQuery, String> column(String heading,
                                                    java.util.function.Function<SavedQuery, String> value,
                                                    double width) {
        TableColumn<SavedQuery, String> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void save() {
        try {
            service.saveQuery(title.getText(), sql.getText());
            clearSelection();
            refresh();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void update() {
        SavedQuery selected = queries.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            service.updateSavedQuery(selected.id(), title.getText(), sql.getText());
            clearSelection();
            refresh();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void delete() {
        SavedQuery selected = queries.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the saved query '" + selected.title() + "'?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("Delete saved query");
        Optional<ButtonType> response = confirmation.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            try {
                service.deleteSavedQuery(selected.id());
                clearSelection();
                refresh();
            } catch (Exception exception) {
                showError(exception.getMessage());
            }
        }
    }

    private void loadSelected() {
        SavedQuery selected = queries.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choose a saved query first.");
            return;
        }
        onLoad.accept(selected.sql());
    }

    private void clearSelection() {
        queries.getSelectionModel().clearSelection();
        title.clear();
        sql.clear();
        update.setDisable(true);
        delete.setDisable(true);
    }

    private void refresh() {
        queries.setItems(FXCollections.observableArrayList(service.savedQueries()));
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
