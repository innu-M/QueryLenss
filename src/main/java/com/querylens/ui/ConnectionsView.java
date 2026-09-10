package com.querylens.ui;

import com.querylens.workspace.facade.QueryWorkspaceService;
import com.querylens.workspace.SavedConnection;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public final class ConnectionsView extends VBox {
    private final QueryWorkspaceService service;
    private final Runnable onConnectionSaved;
    private final TextField name = new TextField();
    private final TextField path = new TextField();
    private final ListView<SavedConnection> connections = new ListView<>();

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
        return new VBox(8, new Label("Name"), name, new Label("Database"), new HBox(8, path, browse), save);
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
            name.clear();
            path.clear();
            refresh();
            onConnectionSaved.run();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void refresh() {
        connections.setItems(FXCollections.observableArrayList(service.connections()));
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}

