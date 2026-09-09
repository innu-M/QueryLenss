package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.AnalysisResultBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class QueryAnalysisTemplate {
    private static final Pattern QUERY_TYPE = Pattern.compile("^\\s*(SELECT|INSERT|UPDATE|DELETE)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_NAME = Pattern.compile("\\b(?:FROM|JOIN|INTO|UPDATE)\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);

    public final AnalysisResult analyze(String sql) {
        if (sql == null || sql.isBlank()) return new AnalysisResultBuilder().planSteps(List.of("Input")).build();
        String queryType = findQueryType(sql);
        List<String> tables = findTables(sql);
        int joinCount = countMatches(JOIN, sql);
        boolean hasWhereClause = sql.toUpperCase(Locale.ROOT).contains("WHERE");
        boolean usesSelectStar = sql.toUpperCase(Locale.ROOT).matches("(?s).*SELECT\\s+\\*.*");
        int score = baseComplexity(joinCount, hasWhereClause, usesSelectStar) + additionalComplexity(sql);
        String risk = score >= 4 ? "High" : score >= 2 ? "Medium" : "Low";
        return new AnalysisResultBuilder()
                .queryType(queryType).tables(tables).joinCount(joinCount).hasWhereClause(hasWhereClause)
                .whereColumns(findWhereColumns(sql)).complexityScore(score).riskLevel(risk)
                .usesSelectStar(usesSelectStar).planSteps(List.of("Plan will be captured during execution.")).build();
    }

    protected abstract int additionalComplexity(String sql);

    private int baseComplexity(int joinCount, boolean hasWhereClause, boolean usesSelectStar) {
        return joinCount * 2 + (hasWhereClause ? 1 : 0) + (usesSelectStar ? 1 : 0);
    }

    private String findQueryType(String sql) {
        Matcher matcher = QUERY_TYPE.matcher(sql);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "Unknown";
    }

    private List<String> findTables(String sql) {
        List<String> tables = new ArrayList<>();
        Matcher matcher = TABLE_NAME.matcher(sql);
        while (matcher.find() && !tables.contains(matcher.group(1))) tables.add(matcher.group(1));
        return tables;
    }

    private int countMatches(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) count++;
        return count;
    }

    private List<String> findWhereColumns(String sql) {
        Matcher whereMatcher = Pattern.compile("\\bWHERE\\b(.*?)(?:\\bORDER\\s+BY\\b|\\bGROUP\\s+BY\\b|\\bLIMIT\\b|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(sql);
        if (!whereMatcher.find()) return List.of();
        Pattern pattern = Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=|<|>|<=|>=|LIKE)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(whereMatcher.group(1));
        List<String> columns = new ArrayList<>();
        while (matcher.find() && !columns.contains(matcher.group(1))) columns.add(matcher.group(1));
        return columns;
    }
}
