package com.querylens.workspace.execution;

import com.querylens.persistence.query.QueryAnalysisRepository;
import com.querylens.persistence.query.QueryHistoryRepository;
import com.querylens.persistence.recommendation.RecommendationRepository;
import com.querylens.recommendation.model.Recommendation;
import com.querylens.recommendation.service.RecommendationEngine;
import com.querylens.workspace.model.QueryAnalysis;
import com.querylens.workspace.model.QueryExecutionResult;
import com.querylens.workspace.model.QueryHistoryEntry;
import com.querylens.workspace.analysis.SqlClassifier;
import com.querylens.workspace.model.SqlQueryType;
import com.querylens.workspace.analysis.RegexQueryAnalyzer;
import com.querylens.workspace.validation.chain.SqlValidationChain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class QueryExecutionWorkflow {
    private final QueryHistoryRepository history;
    private final QueryAnalysisRepository analyses;
    private final RecommendationRepository recommendations;
    private final RecommendationEngine recommendationEngine;
    private final SqlValidationChain validation;
    private final SqlClassifier classifier;
    private final RegexQueryAnalyzer analyzer;
    private final AbstractQueryExecutor executor;

    public QueryExecutionWorkflow(QueryHistoryRepository history,
                           QueryAnalysisRepository analyses,
                           RecommendationRepository recommendations,
                           RecommendationEngine recommendationEngine,
                           SqlValidationChain validation,
                           SqlClassifier classifier,
                           RegexQueryAnalyzer analyzer,
                           AbstractQueryExecutor executor) {
        this.history = history;
        this.analyses = analyses;
        this.recommendations = recommendations;
        this.recommendationEngine = recommendationEngine;
        this.validation = validation;
        this.classifier = classifier;
        this.analyzer = analyzer;
        this.executor = executor;
    }

    public boolean requiresMutationConfirmation(String sql) {
        validation.validate(sql);
        return classifier.classify(sql) != SqlQueryType.SELECT;
    }

    public List<QueryHistoryEntry> recentHistory() {
        return history.recent(20);
    }

    public QueryExecutionResult run(Path databasePath, String sql) {
        validateDatabase(databasePath);
        validation.validate(sql);

        SqlQueryType type = classifier.classify(sql);
        AbstractQueryExecutor.RawQueryResult rawResult = executor.execute(databasePath, sql, type);
        QueryAnalysis analysis = analyzer.analyze(sql, type);
        List<Recommendation> generatedRecommendations = saveExecution(sql, type, rawResult, analysis);

        return new QueryExecutionResult(
                rawResult.returnsRows(),
                rawResult.columns(),
                rawResult.rows(),
                rawResult.affectedRows(),
                rawResult.durationMillis(),
                analysis,
                generatedRecommendations
        );
    }

    private List<Recommendation> saveExecution(String sql,
                                                SqlQueryType type,
                                                AbstractQueryExecutor.RawQueryResult rawResult,
                                                QueryAnalysis analysis) {
        long historyId = history.save(sql, type, rawResult.durationMillis());
        long analysisId = analyses.save(historyId, analysis);
        return recommendations.saveAll(analysisId, recommendationEngine.generate(analysis));
    }

    private void validateDatabase(Path databasePath) {
        if (databasePath == null || !Files.isRegularFile(databasePath)) {
            throw new IllegalArgumentException("Choose an existing SQLite database file.");
        }
    }
}


