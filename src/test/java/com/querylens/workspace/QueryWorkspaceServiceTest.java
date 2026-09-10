package com.querylens.workspace;

import com.querylens.persistence.core.DatabaseInitializer;
import com.querylens.workspace.facade.QueryWorkspaceService;
import com.querylens.workspace.model.QueryExecutionResult;
import com.querylens.workspace.model.SqlQueryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryWorkspaceServiceTest {
    @TempDir
    Path directory;

    private QueryWorkspaceService service;
    private Path targetDatabase;

    @BeforeEach
    void setUp() throws Exception {
        Path workspaceDatabase = directory.resolve("workspace.db");
        new DatabaseInitializer().initialize(workspaceDatabase);
        targetDatabase = directory.resolve("target.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + targetDatabase);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE orders (id INTEGER PRIMARY KEY, customer TEXT)");
            statement.execute("INSERT INTO orders(customer) VALUES ('Amina'), ('Rafi')");
        }
        service = new QueryWorkspaceService(workspaceDatabase);
    }

    @Test
    void savesConnectionsRunsSelectsAndRecordsHistory() {
        service.saveConnection("Demo orders", targetDatabase);

        QueryExecutionResult result = service.run(targetDatabase, "SELECT id, customer FROM orders WHERE id = 1");

        assertEquals(1, service.connections().size());
        assertTrue(result.returnsRows());
        assertEquals(List.of("id", "customer"), result.columns());
        assertEquals(1, result.rows().size());
        assertEquals(SqlQueryType.SELECT, result.analysis().queryType());
        assertEquals(List.of("orders"), result.analysis().tables());
        assertEquals(List.of("id"), result.analysis().filteredColumns());
        assertEquals(1, service.recentHistory().size());
    }

    @Test
    void updatesAndDeletesSavedConnections() throws Exception {
        Path replacementDatabase = directory.resolve("replacement.db");
        try (var ignored = DriverManager.getConnection("jdbc:sqlite:" + replacementDatabase)) {
            // Opening SQLite creates a valid empty database file for connection validation.
        }
        var saved = service.saveConnection("Original", targetDatabase);

        service.updateConnection(saved.id(), "Replacement", replacementDatabase);
        assertEquals("Replacement", service.connections().getFirst().displayName());
        assertEquals(replacementDatabase.toAbsolutePath(), service.connections().getFirst().databasePath());

        service.deleteConnection(saved.id());
        assertTrue(service.connections().isEmpty());
    }

    @Test
    void savesValidatesAndUpdatesReusableQueries() {
        var saved = service.saveQuery("Order lookup", "SELECT * FROM orders WHERE id = 1");

        assertEquals("Order lookup", service.savedQueries().getFirst().title());
        assertThrows(IllegalArgumentException.class, () -> service.saveQuery("", "SELECT 1"));
        assertThrows(IllegalArgumentException.class, () -> service.saveQuery("Empty", "   "));

        service.updateSavedQuery(saved.id(), "Customer lookup", "SELECT * FROM orders WHERE customer = 'Amina'");
        assertEquals("Customer lookup", service.savedQueries().getFirst().title());

        service.deleteSavedQuery(saved.id());
        assertTrue(service.savedQueries().isEmpty());
    }

    @Test
    void savesRecommendationsAndAllowsOneFinalDecision() {
        QueryExecutionResult result = service.run(targetDatabase, "SELECT * FROM orders WHERE id = 1");

        assertEquals(2, result.recommendations().size());
        long recommendationId = result.recommendations().getFirst().id();
        service.applyRecommendation(recommendationId);

        assertEquals(com.querylens.recommendation.model.RecommendationStatus.APPLIED,
                service.recommendations().stream().filter(item -> item.id() == recommendationId).findFirst().orElseThrow().status());
        assertThrows(IllegalStateException.class, () -> service.dismissRecommendation(recommendationId));

        service.deleteRecommendation(recommendationId);
        assertTrue(service.recommendations().stream().noneMatch(item -> item.id() == recommendationId));
    }

    @Test
    void runsMutationsButBlocksMultipleAndSchemaChangingStatements() {
        QueryExecutionResult result = service.run(targetDatabase, "UPDATE orders SET customer = 'Nila' WHERE id = 1");

        assertEquals(1, result.affectedRows());
        assertTrue(service.requiresMutationConfirmation("DELETE FROM orders WHERE id = 2"));
        assertFalse(service.requiresMutationConfirmation("SELECT * FROM orders"));
        assertThrows(IllegalArgumentException.class, () -> service.run(targetDatabase, "SELECT * FROM orders; DELETE FROM orders"));
        assertThrows(IllegalArgumentException.class, () -> service.run(targetDatabase, "DROP TABLE orders"));
    }
}
