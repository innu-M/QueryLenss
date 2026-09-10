package com.querylens.alternative.adapter;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SQLiteIndexCatalogProvider implements IndexCatalogProvider {
    @Override
    public List<String> findIndexes(Path databasePath, String tableName) {
        String escapedTable = tableName.replace("'", "''");
        List<String> indexes = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA index_list('" + escapedTable + "')")) {
            while (result.next()) indexes.add(result.getString("name"));
            return List.copyOf(indexes);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read SQLite indexes.", exception);
        }
    }
}
