package com.querylens.repository;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.sql.SQLException;
import java.util.List;

public class AnalysisRepository {

    public void save(long queryId, AnalysisResult analysis, List<Recommendation> recommendations) {
        try (var connection = DatabaseManager.openHistoryConnection()) {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO query_analysis (query_id, query_type, tables_used, join_count, complexity_score, risk_level)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setLong(1, queryId);
                statement.setString(2, analysis.queryType());
                statement.setString(3, String.join(", ", analysis.tables()));
                statement.setInt(4, analysis.joinCount());
                statement.setInt(5, analysis.complexityScore());
                statement.setString(6, analysis.riskLevel());
                statement.executeUpdate();
            }

            try (var statement = connection.prepareStatement("""
                    INSERT INTO recommendations (query_id, recommendation_type, description, priority, status)
                    VALUES (?, ?, ?, ?, 'PENDING')
                    """)) {
                for (Recommendation recommendation : recommendations) {
                    statement.setLong(1, queryId);
                    statement.setString(2, recommendation.type());
                    statement.setString(3, recommendation.description());
                    statement.setString(4, recommendation.priority());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save analysis results.", exception);
        }
    }
}
