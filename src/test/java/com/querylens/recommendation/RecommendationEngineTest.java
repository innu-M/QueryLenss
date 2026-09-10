package com.querylens.recommendation;

import com.querylens.workspace.analysis.SimpleQueryAnalyzer;
import com.querylens.workspace.SqlQueryType;
import com.querylens.recommendation.strategy.RecommendationStrategyFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEngineTest {
    private final RecommendationEngine engine = new RecommendationEngine(new RecommendationStrategyFactory());
    private final SimpleQueryAnalyzer analyzer = new SimpleQueryAnalyzer();

    @Test
    void createsSuggestionsForSelectStarFiltersSortingAndJoins() {
        List<String> suggestions = engine.generate(analyzer.analyze(
                "SELECT * FROM orders JOIN customers ON orders.customer_id = customers.id WHERE status = 'OPEN' ORDER BY created_at",
                SqlQueryType.SELECT));

        assertTrue(suggestions.size() >= 4);
        assertTrue(suggestions.get(0).contains("SELECT *"));
        assertTrue(suggestions.stream().anyMatch(message -> message.contains("status")));
        assertTrue(suggestions.stream().anyMatch(message -> message.contains("ORDER BY")));
        assertTrue(suggestions.stream().anyMatch(message -> message.contains("join")));
    }
}

