package com.undercontroll.domain.exception;

import java.util.List;

public class StockImportComponentNotFoundException extends RuntimeException {

    public static final String CODE = "COMPONENT_NOT_FOUND";

    private final List<FieldViolation> violations;

    public StockImportComponentNotFoundException(List<FieldViolation> violations) {
        super("Referenced component does not exist");
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }
}
