package com.querylens.validation;

import com.querylens.alternative.QueryCandidate;

import java.util.regex.Pattern;

public class ReadOnlyValidator extends CandidateValidator {
    private static final Pattern WRITE_KEYWORD = Pattern.compile(
            "\\b(?:INSERT|UPDATE|DELETE|REPLACE|CREATE|ALTER|DROP|ATTACH|DETACH|VACUUM|REINDEX|PRAGMA)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected ValidationResult check(QueryCandidate candidate) {
        String withoutStringLiterals = candidate.sql().replaceAll("'(?:''|[^'])*'", "''");
        return WRITE_KEYWORD.matcher(withoutStringLiterals).find()
                ? ValidationResult.rejected("The candidate contains an operation that is not read-only.")
                : ValidationResult.accepted();
    }
}
