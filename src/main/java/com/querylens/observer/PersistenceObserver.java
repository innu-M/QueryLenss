package com.querylens.observer;

import com.querylens.model.QueryExecutionResult;
import com.querylens.repository.AnalysisRepository;
import com.querylens.repository.QueryHistoryRepository;

public class PersistenceObserver implements QueryExecutionObserver {
    private final QueryHistoryRepository historyRepository = new QueryHistoryRepository();
    private final AnalysisRepository analysisRepository = new AnalysisRepository();

    @Override
    public void onQueryExecuted(QueryExecutionResult result) {
        long queryId = historyRepository.save(result.databasePath(), result.sql(), result.executionTimeMs(),
                result.slow() ? "SLOW" : "SUCCESS");
        analysisRepository.save(queryId, result.analysis(), result.recommendations());
    }
}
