package com.querylens.workspace.validation.chain;

import java.util.List;
import java.util.Locale;

public final class SqlValidationChain {
    private final List<SqlValidationRule> rules = List.of(
            this::validateNotBlank,
            this::validateSingleStatement,
            this::validateSupportedStatement
    );

    public void validate(String sql) {
        rules.forEach(rule -> rule.validate(sql));
    }

    private void validateNotBlank(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Enter a SQL statement.");
        }
    }

    private void validateSingleStatement(String sql) {
        String trimmed = sql.strip();
        String withoutTrailingSemicolon = trimmed.endsWith(";")
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
        if (withoutTrailingSemicolon.contains(";")) {
            throw new IllegalArgumentException("Run one SQL statement at a time.");
        }
    }

    private void validateSupportedStatement(String sql) {
        String keyword = sql.stripLeading().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        if (isSchemaChanging(keyword)) {
            throw new IllegalArgumentException(
                    "This workspace blocks schema-changing statements. Use SELECT, INSERT, UPDATE, or DELETE."
            );
        }
    }

    private boolean isSchemaChanging(String keyword) {
        return keyword.equals("DROP")
                || keyword.equals("ALTER")
                || keyword.equals("ATTACH")
                || keyword.equals("VACUUM");
    }
}



