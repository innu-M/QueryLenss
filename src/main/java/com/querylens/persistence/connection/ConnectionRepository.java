package com.querylens.persistence.connection;

import com.querylens.persistence.core.AbstractWorkspaceRepository;
import com.querylens.workspace.model.SavedConnection;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionRepository extends AbstractWorkspaceRepository {

    public ConnectionRepository(Path workspaceDatabase) {
        super(workspaceDatabase);
    }

    public SavedConnection save(String displayName, Path databasePath) {
        String sql = "INSERT INTO database_connections(display_name, database_path) VALUES (?, ?)";
        try (var connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, displayName.strip());
            statement.setString(2, databasePath.toAbsolutePath().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new SavedConnection(keys.getLong(1), displayName.strip(), databasePath.toAbsolutePath());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save this database connection.", exception);
        }
    }

    public List<SavedConnection> findAll() {
        List<SavedConnection> connections = new ArrayList<>();
        try (var connection = openConnection();
             var statement = connection.prepareStatement("SELECT id, display_name, database_path FROM database_connections ORDER BY display_name");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                connections.add(new SavedConnection(rows.getLong("id"), rows.getString("display_name"), Path.of(rows.getString("database_path"))));
            }
            return List.copyOf(connections);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load saved database connections.", exception);
        }
    }

    public SavedConnection update(long id, String displayName, Path databasePath) {
        String sql = "UPDATE database_connections SET display_name = ?, database_path = ? WHERE id = ?";
        Path absolutePath = databasePath.toAbsolutePath();
        try (var connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, displayName.strip());
            statement.setString(2, absolutePath.toString());
            statement.setLong(3, id);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Database connection not found.");
            return new SavedConnection(id, displayName.strip(), absolutePath);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not update this database connection.", exception);
        }
    }

    public void delete(long id) {
        try (var connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM database_connections WHERE id = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Database connection not found.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not delete this database connection.", exception);
        }
    }
}

