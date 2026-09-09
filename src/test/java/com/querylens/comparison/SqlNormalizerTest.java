package com.querylens.comparison;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlNormalizerTest {
    @Test
    void groupsQueriesThatDifferOnlyByLiteralValues() {
        SqlNormalizer normalizer = new SqlNormalizer();

        assertEquals(normalizer.normalize("SELECT * FROM sailors WHERE rating = 8"),
                normalizer.normalize(" select  *  from sailors where rating = 10;"));
    }
}
