package com.querylens.workspace.analysis;
import com.querylens.workspace.QueryAnalysis;
import com.querylens.workspace.SqlQueryType;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimpleQueryAnalyzer {
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?i)\\b(?:FROM|JOIN|INTO|UPDATE)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern FILTER_COLUMN_PATTERN = Pattern.compile("(?i)\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?:=|<|>|<=|>=|LIKE\\b|IN\\b)");

    public QueryAnalysis analyze(String sql, SqlQueryType type) {
        String upper = sql.toUpperCase(Locale.ROOT);
        List<String> tables = tables(sql);
        int joins = count(upper, " JOIN ");
        boolean where = upper.contains(" WHERE ");
        boolean selectAll = upper.matches("(?s).*\\bSELECT\\s+\\*\\s+.*");
        List<String> filteredColumns = filteredColumns(sql);
        boolean orderingOrGrouping = upper.contains(" ORDER BY ") || upper.contains(" GROUP BY ");
        int score = joins * 2 + (where ? 0 : 1) + (selectAll ? 1 : 0) + Math.max(0, tables.size() - 1);
        String risk = score >= 4 ? "HIGH" : score >= 2 ? "MEDIUM" : "LOW";
        return new QueryAnalysis(type, tables, joins, where, selectAll, filteredColumns, orderingOrGrouping, score, risk);
    }

    private List<String> filteredColumns(String sql) {
        List<String> columns = new ArrayList<>();
        Matcher matcher = FILTER_COLUMN_PATTERN.matcher(sql);
        while (matcher.find() && !columns.contains(matcher.group(1))) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    private List<String> tables(String sql) {
        List<String> tables = new ArrayList<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        while (matcher.find() && !tables.contains(matcher.group(1))) {
            tables.add(matcher.group(1));
        }
        return tables;
    }

    private int count(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }
}




