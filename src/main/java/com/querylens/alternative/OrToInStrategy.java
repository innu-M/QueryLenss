package com.querylens.alternative;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OrToInStrategy implements AlternativeQueryStrategy {
    private static final String VALUE = "('(?:''|[^'])*'|[-+]?\\d+(?:\\.\\d+)?)";
    private static final Pattern SAME_COLUMN_OR = Pattern.compile(
            "(?i)\\b([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*" + VALUE
                    + "\\s+OR\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*" + VALUE);

    @Override
    public List<GeneratedQueryCandidate> generate(QueryStructure query, List<String> indexes) {
        Matcher matcher = SAME_COLUMN_OR.matcher(query.sql());
        if (!matcher.find() || !matcher.group(1).equalsIgnoreCase(matcher.group(3))) return List.of();
        String replacement = matcher.group(1) + " IN (" + matcher.group(2) + ", " + matcher.group(4) + ")";
        String rewritten = query.sql().substring(0, matcher.start()) + replacement + query.sql().substring(matcher.end());
        return List.of(new GeneratedQueryCandidate(
                "OR-to-IN rewrite",
                rewritten,
                "Combines equality checks on the same column into IN, which can give SQLite a simpler searchable predicate."));
    }
}
