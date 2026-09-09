package com.querylens.repository;

import com.querylens.model.DatabaseConnectionEntry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnectionRepository {

    public void record(String databasePath) {
        save(0, databasePath);
    }

    public void save(long id, String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException("Enter a database path.");
        }
        boolean isNew = id == 0;
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement(isNew
                     ? "INSERT OR IGNORE INTO database_connections (database_path) VALUES (?)"
                     : "UPDATE database_connections SET database_path = ? WHERE connection_id = ?")) {
            statement.setString(1, databasePath);
            if (!isNew) statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the database connection.", exception);
        }
    }

    public List<DatabaseConnectionEntry> findAll() {
        List<DatabaseConnectionEntry> entries = new ArrayList<>();
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement("""
                     SELECT connection_id, database_path, created_at FROM database_connections ORDER BY connection_id DESC
                     """);
             var results = statement.executeQuery()) {
            while (results.next()) {
                entries.add(new DatabaseConnectionEntry(results.getLong("connection_id"),
                        results.getString("database_path"), results.getString("created_at")));
            }
            return entries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load database connections.", exception);
        }
    }

    public void delete(long id) {
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement("DELETE FROM database_connections WHERE connection_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete the database connection.", exception);
        }
    }
}
