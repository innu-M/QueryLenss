package com.querylens.persistence.query;

import com.querylens.persistence.core.AbstractWorkspaceRepository;
import com.querylens.workspace.model.QueryAnalysis;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;

public final class QueryAnalysisRepository extends AbstractWorkspaceRepository {

    public QueryAnalysisRepository(Path workspaceDatabase) {
        super(workspaceDatabase);
    }

    public long save(long historyId, QueryAnalysis analysis) {
        String insert = "INSERT INTO query_analyses(history_id, complexity_score, risk_level, plan_text) VALUES (?, ?, ?, ?)";
        try (var connection = openConnection();
             var statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, historyId);
            statement.setInt(2, analysis.complexityScore());
            statement.setString(3, analysis.riskLevel());
            statement.setString(4, "Tables: " + analysis.tables());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new IllegalStateException("Could not retrieve query analysis ID.");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save query analysis.", exception);
        }
    }
}


