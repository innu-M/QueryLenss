package com.querylens.persistence.query;

import com.querylens.persistence.core.AbstractWorkspaceRepository;
import com.querylens.workspace.model.QueryHistoryEntry;
import com.querylens.workspace.model.SqlQueryType;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class QueryHistoryRepository extends AbstractWorkspaceRepository {

    public QueryHistoryRepository(Path workspaceDatabase) {
        super(workspaceDatabase);
    }

    public long save(String sql, SqlQueryType type, long durationMillis) {
        String insert = "INSERT INTO query_history(sql_text, query_type, duration_ms) VALUES (?, ?, ?)";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, sql);
            statement.setString(2, type.name());
            statement.setLong(3, durationMillis);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new IllegalStateException("Could not retrieve query history ID.");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save query history.", exception);
        }
    }

    public List<QueryHistoryEntry> recent(int limit) {
        List<QueryHistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, sql_text, query_type, duration_ms, executed_at FROM query_history ORDER BY id DESC LIMIT ?";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(query)) {
            statement.setInt(1, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    entries.add(new QueryHistoryEntry(rows.getLong("id"), rows.getString("sql_text"),
                            SqlQueryType.valueOf(rows.getString("query_type")), rows.getLong("duration_ms"), rows.getString("executed_at")));
                }
            }
            return List.copyOf(entries);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load query history.", exception);
        }
    }
}


