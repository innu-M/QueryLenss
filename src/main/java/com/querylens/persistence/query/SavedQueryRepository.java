package com.querylens.persistence.query;

import com.querylens.persistence.core.AbstractWorkspaceRepository;
import com.querylens.workspace.model.SavedQuery;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the user's reusable SQL statements separately from execution history.
 */
public final class SavedQueryRepository extends AbstractWorkspaceRepository {
    public SavedQueryRepository(Path workspaceDatabase) {
        super(workspaceDatabase);
    }

    public SavedQuery save(String title, String sql) {
        String insert = "INSERT INTO saved_queries(title, sql_text) VALUES (?, ?)";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, sql);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new IllegalStateException("Could not retrieve saved query ID.");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save the query. Choose a different title if it already exists.", exception);
        }
    }

    public SavedQuery update(long id, String title, String sql) {
        String update = "UPDATE saved_queries SET title = ?, sql_text = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(update)) {
            statement.setString(1, title);
            statement.setString(2, sql);
            statement.setLong(3, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Saved query was not found.");
            }
            return findById(id);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not update the saved query. Choose a different title if it already exists.", exception);
        }
    }

    public void delete(long id) {
        try (var connection = openConnection();
             var statement = connection.prepareStatement("DELETE FROM saved_queries WHERE id = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Saved query was not found.");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not delete the saved query.", exception);
        }
    }

    public List<SavedQuery> findAll() {
        List<SavedQuery> queries = new ArrayList<>();
        String select = "SELECT id, title, sql_text, updated_at FROM saved_queries ORDER BY title COLLATE NOCASE";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(select);
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                queries.add(map(rows));
            }
            return List.copyOf(queries);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load saved queries.", exception);
        }
    }

    private SavedQuery findById(long id) {
        String select = "SELECT id, title, sql_text, updated_at FROM saved_queries WHERE id = ?";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(select)) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) {
                if (rows.next()) {
                    return map(rows);
                }
            }
            throw new IllegalArgumentException("Saved query was not found.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load the saved query.", exception);
        }
    }

    private SavedQuery map(ResultSet row) throws Exception {
        return new SavedQuery(row.getLong("id"), row.getString("title"),
                row.getString("sql_text"), row.getString("updated_at"));
    }
}
