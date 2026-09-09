package com.querylens.repository;

import com.querylens.model.IndexEntry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IndexCatalogRepository {

    public List<IndexEntry> findAll(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException("Enter a SQLite database path.");
        }
        List<IndexEntry> indexes = new ArrayList<>();
        try (var connection = DatabaseManager.openTargetConnection(databasePath);
             var tables = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'");
             var tableResults = tables.executeQuery()) {
            while (tableResults.next()) {
                String tableName = tableResults.getString("name");
                try (var statement = connection.createStatement();
                     var indexResults = statement.executeQuery("PRAGMA index_list('" + tableName + "')")) {
                    while (indexResults.next()) {
                        String indexName = indexResults.getString("name");
                        boolean unique = indexResults.getInt("unique") == 1;
                        try (var info = connection.createStatement();
                             var columnResults = info.executeQuery("PRAGMA index_info('" + indexName + "')")) {
                            while (columnResults.next()) {
                                indexes.add(new IndexEntry(tableName, indexName, columnResults.getString("name"), unique));
                            }
                        }
                    }
                }
            }
            return indexes;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read indexes from this database.", exception);
        }
    }
}
