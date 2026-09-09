package com.querylens.validation;

import com.querylens.alternative.QueryCandidate;

import java.util.regex.Pattern;

public class SelectOnlyValidator extends CandidateValidator {
    private static final Pattern SELECT = Pattern.compile("^\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);

    @Override
    protected ValidationResult check(QueryCandidate candidate) {
        return SELECT.matcher(candidate.sql()).find()
                ? ValidationResult.accepted()
                : ValidationResult.rejected("Automatic comparison is limited to SELECT queries.");
    }
}
