package com.querylens.persistence;

import com.querylens.persistence.comparison.ComparisonHistoryRepository;
import com.querylens.persistence.core.DatabaseInitializer;
import com.querylens.benchmark.model.RankingStrategy;
import com.querylens.history.model.ComparisonCandidateDraft;
import com.querylens.history.model.ComparisonDraft;
import com.querylens.history.model.ComparisonHistorySummary;
import com.querylens.history.model.ComparisonSessionEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonHistoryRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private Path databasePath;
    private ComparisonHistoryRepository repository;

    @BeforeEach
    void setUp() {
        databasePath = temporaryDirectory.resolve("querylens.db");
        new DatabaseInitializer().initialize(databasePath);
        repository = new ComparisonHistoryRepository(databasePath);
    }

    @Test
    void savesAndReadsCompleteComparison() {
        long sessionId = repository.save(comparison("SELECT * FROM sailors"));

        List<ComparisonSessionEntry> sessions = repository.findSessions("");
        assertEquals(1, sessions.size());
        assertEquals(sessionId, sessions.getFirst().id());
        assertEquals("SELECT * FROM sailors", sessions.getFirst().title());
        assertEquals(2, sessions.getFirst().candidateCount());
        assertEquals("Projection rewrite", sessions.getFirst().winnerLabel());
        assertEquals(200, sessions.getFirst().originalMedianNs());
        assertEquals(100, sessions.getFirst().winnerMedianNs());
        assertEquals(50.0, sessions.getFirst().improvementPercent(), 0.001);

        var candidates = repository.findCandidates(sessionId);
        assertEquals(2, candidates.size());
        assertEquals("Projection rewrite", candidates.getFirst().label());
        assertEquals(3, candidates.getFirst().sampleCount());
        assertEquals(List.of(90L, 100L, 110L), repository.findRunDurations(candidates.getFirst().id()));
    }

    @Test
    void searchesSqlAndDatabasePathCaseInsensitively() {
        repository.save(comparison("SELECT * FROM sailors"));

        assertEquals(1, repository.findSessions("SAILORS").size());
        assertEquals(1, repository.findSessions("demo.db").size());
        assertTrue(repository.findSessions("boats").isEmpty());
    }

    @Test
    void renamesAndSearchesComparisonSessions() {
        long sessionId = repository.save(comparison("SELECT * FROM sailors"));

        repository.rename(sessionId, "Sailor projection benchmark");

        assertEquals("Sailor projection benchmark", repository.findSessions("").getFirst().title());
        assertEquals(1, repository.findSessions("PROJECTION BENCHMARK").size());
    }

    @Test
    void summarizesSavedComparisons() {
        repository.save(comparison("SELECT * FROM sailors"));

        ComparisonHistorySummary summary = repository.summarize();
        assertEquals(1, summary.totalSessions());
        assertEquals(2, summary.verifiedCandidates());
        assertEquals(50.0, summary.averageImprovementPercent(), 0.001);
        assertEquals(50.0, summary.bestImprovementPercent(), 0.001);
    }

    @Test
    void deletingSessionCascadesToCandidatesAndRuns() throws Exception {
        long sessionId = repository.save(comparison("SELECT * FROM sailors"));
        repository.delete(sessionId);

        assertTrue(repository.findSessions("").isEmpty());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             Statement statement = connection.createStatement()) {
            assertEquals(0, count(statement, "comparison_candidates"));
            assertEquals(0, count(statement, "benchmark_runs"));
        }
    }

    private ComparisonDraft comparison(String sql) {
        return new ComparisonDraft(
                "/tmp/demo.db",
                sql,
                RankingStrategy.MEDIAN,
                List.of(
                        new ComparisonCandidateDraft(
                                "Original", sql, List.of(190L, 200L, 210L), true,
                                "VERIFIED", 2, "Uses a full scan.", "SCAN sailors"),
                        new ComparisonCandidateDraft(
                                "Projection rewrite", "SELECT name FROM sailors", List.of(90L, 100L, 110L), true,
                                "VERIFIED", 1, "Reads fewer columns.", "SCAN sailors USING COVERING INDEX")));
    }

    private long count(Statement statement, String table) throws Exception {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.getLong(1);
        }
    }
}
