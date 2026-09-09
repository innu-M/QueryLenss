package com.querylens.validation;

import com.querylens.alternative.QueryCandidate;

public abstract class CandidateValidator {
    private CandidateValidator next;

    public CandidateValidator linkWith(CandidateValidator nextValidator) {
        next = nextValidator;
        return nextValidator;
    }

    public final ValidationResult validate(QueryCandidate candidate) {
        ValidationResult result = check(candidate);
        if (!result.valid() || next == null) return result;
        return next.validate(candidate);
    }

    protected abstract ValidationResult check(QueryCandidate candidate);
}
