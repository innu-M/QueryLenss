package com.querylens.ui.connection;

import com.querylens.workspace.facade.QueryWorkspaceService;
import com.querylens.workspace.model.SavedConnection;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public final class ConnectionsView extends VBox {
    private final QueryWorkspaceService service;
    private final Runnable onConnectionSaved;
    private final TextField name = new TextField();
    private final TextField path = new TextField();
    private final ListView<SavedConnection> connections = new ListView<>();
    private final Button update = new Button("Update selected");
    private final Button delete = new Button("Delete selected");

    public ConnectionsView(QueryWorkspaceService service, Runnable onConnectionSaved) {
        this.service = service;
        this.onConnectionSaved = onConnectionSaved;
        setSpacing(10);
        setPadding(new Insets(20));
        getChildren().addAll(new Label("Saved database connections"), createForm(), connections);
        VBox.setVgrow(connections, Priority.ALWAYS);
        connections.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(SavedConnection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName() + " — " + item.databasePath());
            }
        });
        connections.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            update.setDisable(selected == null);
            delete.setDisable(selected == null);
            if (selected != null) {
                name.setText(selected.displayName());
                path.setText(selected.databasePath().toString());
            }
        });
        refresh();
    }

    private VBox createForm() {
        name.setPromptText("Connection name, for example Demo orders");
        path.setPromptText("Path to a SQLite database");
        HBox.setHgrow(path, Priority.ALWAYS);
        Button browse = new Button("Browse");
        browse.setOnAction(event -> chooseDatabase());
        Button save = new Button("Save connection");
        save.setOnAction(event -> saveConnection());
        update.setDisable(true);
        update.setOnAction(event -> updateConnection());
        delete.setDisable(true);
        delete.setOnAction(event -> deleteConnection());
        Button clear = new Button("Clear selection");
        clear.setOnAction(event -> clearSelection());
        return new VBox(8, new Label("Name"), name, new Label("Database"),
                new HBox(8, path, browse), new HBox(8, save, update, delete, clear));
    }

    private void chooseDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite databases", "*.db", "*.sqlite", "*.sqlite3"));
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) path.setText(selected.getAbsolutePath());
    }

    private void saveConnection() {
        try {
            service.saveConnection(name.getText(), Path.of(path.getText().strip()));
            clearSelection();
            refresh();
            onConnectionSaved.run();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void updateConnection() {
        SavedConnection selected = connections.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            service.updateConnection(selected.id(), name.getText(), Path.of(path.getText().strip()));
            clearSelection();
            refresh();
            onConnectionSaved.run();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void deleteConnection() {
        SavedConnection selected = connections.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the saved connection '" + selected.displayName()
                        + "'? Existing query history will be kept.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("Delete saved connection");
        Optional<ButtonType> response = confirmation.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            try {
                service.deleteConnection(selected.id());
                clearSelection();
                refresh();
                onConnectionSaved.run();
            } catch (Exception exception) {
                showError(exception.getMessage());
            }
        }
    }

    private void clearSelection() {
        connections.getSelectionModel().clearSelection();
        name.clear();
        path.clear();
        update.setDisable(true);
        delete.setDisable(true);
    }

    private void refresh() {
        connections.setItems(FXCollections.observableArrayList(service.connections()));
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
