package com.querylens.validation;

public record ValidationResult(boolean valid, String message) {
    public static ValidationResult accepted() {
        return new ValidationResult(true, "Candidate accepted.");
    }

    public static ValidationResult rejected(String message) {
        return new ValidationResult(false, message);
    }
}
