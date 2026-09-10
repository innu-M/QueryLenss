package com.querylens.persistence;

import com.querylens.recommendation.state.RecommendationStatus;
import com.querylens.persistence.repository.ConnectionRepository;
import com.querylens.persistence.repository.QueryAnalysisRepository;
import com.querylens.persistence.repository.QueryHistoryRepository;
import com.querylens.persistence.repository.RecommendationRepository;
import com.querylens.workspace.QueryAnalysis;
import com.querylens.workspace.SqlQueryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspacePersistenceRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private ConnectionRepository connections;
    private QueryHistoryRepository history;
    private QueryAnalysisRepository analyses;
    private RecommendationRepository recommendations;

    @BeforeEach
    void setUp() {
        Path workspaceDatabase = temporaryDirectory.resolve("querylens.db");
        new DatabaseInitializer().initialize(workspaceDatabase);
        connections = new ConnectionRepository(workspaceDatabase);
        history = new QueryHistoryRepository(workspaceDatabase);
        analyses = new QueryAnalysisRepository(workspaceDatabase);
        recommendations = new RecommendationRepository(workspaceDatabase);
    }

    @Test
    void savesConnectionsAndListsThemByName() {
        Path zetaDatabase = temporaryDirectory.resolve("zeta.db");
        Path alphaDatabase = temporaryDirectory.resolve("alpha.db");

        connections.save("Zeta", zetaDatabase);
        connections.save("Alpha", alphaDatabase);

        assertEquals(List.of("Alpha", "Zeta"), connections.findAll().stream()
                .map(connection -> connection.displayName())
                .toList());
    }

    @Test
    void savesHistoryAndReturnsMostRecentEntriesFirst() {
        long firstId = history.save("SELECT * FROM orders", SqlQueryType.SELECT, 8);
        long secondId = history.save("UPDATE orders SET status = 'DONE'", SqlQueryType.UPDATE, 11);

        var entries = history.recent(10);
        assertEquals(2, entries.size());
        assertEquals(secondId, entries.getFirst().id());
        assertEquals(SqlQueryType.UPDATE, entries.getFirst().type());
        assertEquals(firstId, entries.get(1).id());
    }

    @Test
    void savesAnalysisAndRecommendationsWithStatusChanges() {
        long historyId = history.save("SELECT * FROM orders WHERE customer_id = 3", SqlQueryType.SELECT, 6);
        long analysisId = analyses.save(historyId, analysis());

        var saved = recommendations.saveAll(analysisId, List.of("Avoid SELECT *.", "Consider an index on customer_id."));
        recommendations.updateStatus(saved.getFirst().id(), RecommendationStatus.APPLIED);

        assertEquals(2, recommendations.findAll().size());
        assertEquals(RecommendationStatus.APPLIED, recommendations.findById(saved.getFirst().id()).status());
        assertEquals(analysisId, recommendations.findById(saved.get(1).id()).analysisId());
        assertThrows(IllegalArgumentException.class, () -> recommendations.findById(999));
    }

    private QueryAnalysis analysis() {
        return new QueryAnalysis(SqlQueryType.SELECT, List.of("orders"), 0, true, true,
                List.of("customer_id"), false, 1, "LOW");
    }
}

