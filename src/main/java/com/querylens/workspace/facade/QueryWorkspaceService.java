package com.querylens.workspace.facade;

import com.querylens.persistence.connection.ConnectionRepository;
import com.querylens.persistence.query.QueryHistoryRepository;
import com.querylens.persistence.query.QueryAnalysisRepository;
import com.querylens.persistence.recommendation.RecommendationRepository;
import com.querylens.recommendation.model.Recommendation;
import com.querylens.recommendation.service.RecommendationEngine;
import com.querylens.recommendation.state.RecommendationState;
import com.querylens.recommendation.state.RecommendationStateFactory;
import com.querylens.recommendation.strategy.RecommendationStrategyFactory;
import com.querylens.workspace.model.QueryExecutionResult;
import com.querylens.workspace.model.QueryHistoryEntry;
import com.querylens.workspace.model.SavedConnection;
import com.querylens.workspace.analysis.SqlClassifier;
import com.querylens.workspace.analysis.RegexQueryAnalyzer;
import com.querylens.workspace.execution.QueryExecutionWorkflow;
import com.querylens.workspace.execution.SQLiteQueryExecutor;
import com.querylens.workspace.validation.chain.SqlValidationChain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class QueryWorkspaceService {
    private final ConnectionRepository connections;
    private final RecommendationRepository recommendations;
    private final QueryExecutionWorkflow executionWorkflow;

    public QueryWorkspaceService(Path workspaceDatabase) {
        this.connections = new ConnectionRepository(workspaceDatabase);
        this.recommendations = new RecommendationRepository(workspaceDatabase);
        this.executionWorkflow = createExecutionWorkflow(workspaceDatabase, recommendations);
    }

    public QueryWorkspaceService(ConnectionRepository connections,
                                 RecommendationRepository recommendations,
                                 QueryExecutionWorkflow executionWorkflow) {
        this.connections = java.util.Objects.requireNonNull(connections);
        this.recommendations = java.util.Objects.requireNonNull(recommendations);
        this.executionWorkflow = java.util.Objects.requireNonNull(executionWorkflow);
    }

    public SavedConnection saveConnection(String displayName, Path databasePath) {
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Enter a name for this connection.");
        if (databasePath == null || !Files.isRegularFile(databasePath)) throw new IllegalArgumentException("Choose an existing SQLite database file.");
        return connections.save(displayName, databasePath);
    }

    public List<SavedConnection> connections() {
        return connections.findAll();
    }

    public List<QueryHistoryEntry> recentHistory() {
        return executionWorkflow.recentHistory();
    }

    public List<Recommendation> recommendations() {
        return recommendations.findAll();
    }

    public void applyRecommendation(long id) {
        updateRecommendation(id, true);
    }

    public void dismissRecommendation(long id) {
        updateRecommendation(id, false);
    }

    public boolean requiresMutationConfirmation(String sql) {
        return executionWorkflow.requiresMutationConfirmation(sql);
    }

    public QueryExecutionResult run(Path databasePath, String sql) {
        return executionWorkflow.run(databasePath, sql);
    }

    private void updateRecommendation(long id, boolean apply) {
        Recommendation recommendation = recommendations.findById(id);
        RecommendationState current = RecommendationStateFactory.from(recommendation.status());
        RecommendationState next = apply ? current.apply() : current.dismiss();
        recommendations.updateStatus(id, next.status());
    }

    private static QueryExecutionWorkflow createExecutionWorkflow(Path workspaceDatabase,
                                                                  RecommendationRepository recommendations) {
        return new QueryExecutionWorkflow(
                new QueryHistoryRepository(workspaceDatabase),
                new QueryAnalysisRepository(workspaceDatabase),
                recommendations,
                new RecommendationEngine(new RecommendationStrategyFactory()),
                new SqlValidationChain(),
                new SqlClassifier(),
                new RegexQueryAnalyzer(),
                new SQLiteQueryExecutor()
        );
    }
}

