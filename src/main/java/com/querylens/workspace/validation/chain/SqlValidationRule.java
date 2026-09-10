package com.querylens.workspace.validation.chain;

@FunctionalInterface
public interface SqlValidationRule {
    void validate(String sql);
}



