package com.querylens.workspace;

import com.querylens.persistence.QueryAnalysisRepository;
import com.querylens.persistence.QueryHistoryRepository;
import com.querylens.persistence.RecommendationRepository;
import com.querylens.recommendation.Recommendation;
import com.querylens.recommendation.RecommendationEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class QueryExecutionWorkflow {
    private final QueryHistoryRepository history;
    private final QueryAnalysisRepository analyses;
    private final RecommendationRepository recommendations;
    private final RecommendationEngine recommendationEngine;
    private final SqlValidationChain validation;
    private final SqlClassifier classifier;
    private final SimpleQueryAnalyzer analyzer;
    private final QueryExecutionTemplate executor;

    QueryExecutionWorkflow(QueryHistoryRepository history,
                           QueryAnalysisRepository analyses,
                           RecommendationRepository recommendations,
                           RecommendationEngine recommendationEngine,
                           SqlValidationChain validation,
                           SqlClassifier classifier,
                           SimpleQueryAnalyzer analyzer,
                           QueryExecutionTemplate executor) {
        this.history = history;
        this.analyses = analyses;
        this.recommendations = recommendations;
        this.recommendationEngine = recommendationEngine;
        this.validation = validation;
        this.classifier = classifier;
        this.analyzer = analyzer;
        this.executor = executor;
    }

    boolean requiresMutationConfirmation(String sql) {
        validation.validate(sql);
        return classifier.classify(sql) != SqlQueryType.SELECT;
    }

    List<QueryHistoryEntry> recentHistory() {
        return history.recent(20);
    }

    QueryExecutionResult run(Path databasePath, String sql) {
        validateDatabase(databasePath);
        validation.validate(sql);

        SqlQueryType type = classifier.classify(sql);
        QueryExecutionTemplate.RawQueryResult rawResult = executor.execute(databasePath, sql, type);
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
                                                QueryExecutionTemplate.RawQueryResult rawResult,
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
