package com.undercontroll.domain.exception;

import java.util.List;

public class StockImportValidationException extends RuntimeException {

    private final List<FieldViolation> violations;

    public StockImportValidationException(List<FieldViolation> violations) {
        super("Invalid stock import review");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
