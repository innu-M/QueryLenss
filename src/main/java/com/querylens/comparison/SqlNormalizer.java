package com.querylens.comparison;

import java.util.Locale;

public class SqlNormalizer {
    public String normalize(String sql) {
        return sql.trim()
                .replaceAll(";+$", "")
                .replaceAll("'(?:''|[^'])*'", "?")
                .replaceAll("\\b\\d+(?:\\.\\d+)?\\b", "?")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}
