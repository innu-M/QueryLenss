package com.querylens.validation;

import com.querylens.alternative.QueryCandidate;

public class SingleStatementValidator extends CandidateValidator {
    @Override
    protected ValidationResult check(QueryCandidate candidate) {
        String withoutTrailingTerminator = candidate.sql().trim().replaceFirst(";\\s*$", "");
        return withoutTrailingTerminator.contains(";")
                ? ValidationResult.rejected("Only one SQL statement can be benchmarked at a time.")
                : ValidationResult.accepted();
    }
}
