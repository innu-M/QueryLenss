package com.querylens.validation;

import com.querylens.alternative.QueryCandidate;

public class CandidateValidationChain {
    private final CandidateValidator first;

    public CandidateValidationChain() {
        SelectOnlyValidator selectOnly = new SelectOnlyValidator();
        selectOnly.linkWith(new SingleStatementValidator())
                .linkWith(new ReadOnlyValidator());
        first = selectOnly;
    }

    public ValidationResult validate(QueryCandidate candidate) {
        return first.validate(candidate);
    }
}
