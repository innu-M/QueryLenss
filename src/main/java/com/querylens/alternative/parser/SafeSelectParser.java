package com.querylens.alternative.parser;

import com.querylens.alternative.model.QueryStructure;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SafeSelectParser {
    private static final Pattern FROM_TABLE = Pattern.compile("(?is)\\bFROM\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern NEXT_CLAUSE = Pattern.compile(
            "(?is)^(WHERE|GROUP\\s+BY|ORDER\\s+BY|LIMIT|OFFSET|HAVING|WINDOW)\\b.*");

    public QueryStructure parse(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Enter a SELECT query.");
        String sql = removeTrailingSemicolon(input.strip());
        if (sql.contains(";")) throw new IllegalArgumentException("Only one SQL statement is allowed.");
        String upper = sql.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("SELECT ") && !upper.startsWith("SELECT\n") && !upper.startsWith("SELECT\t")) {
            throw new IllegalArgumentException("Alternative generation accepts SELECT queries only.");
        }
        if (upper.contains(" JOIN ") || upper.contains(" UNION ") || upper.contains(" INTERSECT ")
                || upper.contains(" EXCEPT ")) {
            throw new IllegalArgumentException("This generator currently accepts one non-joined SELECT table.");
        }

        Matcher matcher = FROM_TABLE.matcher(sql);
        if (!matcher.find()) throw new IllegalArgumentException("A simple FROM table is required.");
        String tableName = matcher.group(1);
        int tableEnd = matcher.end(1);
        if (matcher.find()) throw new IllegalArgumentException("Subqueries are not supported by this generator.");

        String remainder = sql.substring(tableEnd).stripLeading();
        if (!remainder.isEmpty() && !NEXT_CLAUSE.matcher(remainder).matches()) {
            throw new IllegalArgumentException("Table aliases and complex FROM clauses are not supported yet.");
        }
        return new QueryStructure(sql, tableName, tableEnd);
    }

    private String removeTrailingSemicolon(String sql) {
        return sql.endsWith(";") ? sql.substring(0, sql.length() - 1).stripTrailing() : sql;
    }
}
