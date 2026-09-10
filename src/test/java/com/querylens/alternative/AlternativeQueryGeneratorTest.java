package com.querylens.alternative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlternativeQueryGeneratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesOriginalScanIndexesAndSafeRewrite() throws Exception {
        Path database = Files.createFile(temporaryDirectory.resolve("demo.db"));
        AlternativeQueryGenerator generator = new AlternativeQueryGenerator(
                (path, table) -> List.of("idx_rating", "idx_name"));

        List<GeneratedQueryCandidate> candidates = generator.generate(database,
                "SELECT name FROM sailors WHERE rating = 8 OR rating = 10", 10);

        assertEquals(5, candidates.size());
        assertEquals("Original", candidates.getFirst().label());
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.sql().contains("NOT INDEXED")));
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.sql().contains("INDEXED BY \"idx_rating\"")));
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.sql().contains("rating IN (8, 10)")));
    }

    @Test
    void honorsMaximumCandidateLimit() throws Exception {
        Path database = Files.createFile(temporaryDirectory.resolve("demo.db"));
        AlternativeQueryGenerator generator = new AlternativeQueryGenerator(
                (path, table) -> List.of("idx_one", "idx_two"));

        assertEquals(2, generator.generate(database, "SELECT * FROM sailors", 2).size());
    }

    @Test
    void rejectsWritesJoinsAliasesAndMultipleStatements() throws Exception {
        Path database = Files.createFile(temporaryDirectory.resolve("demo.db"));
        AlternativeQueryGenerator generator = new AlternativeQueryGenerator((path, table) -> List.of());

        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(database, "DELETE FROM sailors", 5));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(database, "SELECT * FROM sailors JOIN boats USING (sid)", 5));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(database, "SELECT * FROM sailors s", 5));
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(database, "SELECT * FROM sailors; SELECT 1", 5));
    }
}
