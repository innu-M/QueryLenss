package com.querylens.alternative;

import com.querylens.model.AnalysisResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexPlanStrategy implements QueryRewriteStrategy {
    private static final int MAX_INDEX_CANDIDATES = 6;

    @Override
    public boolean supports(String sql, AnalysisResult analysis) {
        if (!"SELECT".equals(analysis.queryType()) || analysis.tables().size() != 1 || analysis.joinCount() != 0) {
            return false;
        }
        return tableReference(sql, analysis.tables().getFirst()).find();
    }

    @Override
    public List<QueryCandidate> generate(Connection connection, String sql, AnalysisResult analysis) throws SQLException {
        String table = analysis.tables().getFirst();
        List<QueryCandidate> candidates = new ArrayList<>();
        candidates.add(new QueryCandidate("Table scan candidate", insertDirective(sql, table, "NOT INDEXED"),
                "Lets SQLite evaluate the query without an index so its plan can be compared with indexed alternatives."));

        try (var statement = connection.createStatement();
             var indexes = statement.executeQuery("PRAGMA index_list('" + table.replace("'", "''") + "')")) {
            while (indexes.next() && candidates.size() <= MAX_INDEX_CANDIDATES) {
                String indexName = indexes.getString("name");
                String directive = "INDEXED BY \"" + indexName.replace("\"", "\"\"") + "\"";
                candidates.add(new QueryCandidate("Use " + indexName, insertDirective(sql, table, directive),
                        "Forces this existing index for an evidence-based comparison with SQLite's default plan."));
            }
        }
        return candidates;
    }

    private String insertDirective(String sql, String table, String directive) {
        Matcher matcher = tableReference(sql, table);
        if (!matcher.find()) return sql;
        return sql.substring(0, matcher.end()) + " " + directive + sql.substring(matcher.end());
    }

    private Matcher tableReference(String sql, String table) {
        String expression = "(?i)\\bFROM\\s+" + Pattern.quote(table)
                + "(?=\\s*(?:WHERE\\b|ORDER\\s+BY\\b|GROUP\\s+BY\\b|LIMIT\\b|OFFSET\\b|;|$))";
        return Pattern.compile(expression).matcher(sql);
    }
}
