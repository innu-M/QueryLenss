package com.querylens.validation;

import com.querylens.alternative.QueryCandidate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateValidationChainTest {
    private final CandidateValidationChain chain = new CandidateValidationChain();

    @Test
    void acceptsOneReadOnlySelect() {
        assertTrue(chain.validate(new QueryCandidate("Original", "SELECT * FROM sailors;", "Baseline")).valid());
    }

    @Test
    void rejectsWriteAndMultipleStatements() {
        assertFalse(chain.validate(new QueryCandidate("Write", "UPDATE sailors SET rating = 1", "Unsafe")).valid());
        assertFalse(chain.validate(new QueryCandidate("Multiple", "SELECT 1; SELECT 2", "Unsafe")).valid());
    }

    @Test
    void doesNotTreatAKeywordInsideAStringAsAWrite() {
        assertTrue(chain.validate(new QueryCandidate("Text", "SELECT 'delete' AS operation", "Safe text")).valid());
    }
}
