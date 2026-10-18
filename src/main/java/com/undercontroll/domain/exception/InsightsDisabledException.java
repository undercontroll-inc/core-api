package com.undercontroll.domain.exception;

public class InsightsDisabledException extends RuntimeException {

    public static final String CODE = "INSIGHTS_DISABLED";

    public InsightsDisabledException() {
        super("Insight generation is disabled (undercontroll.ai.insights.provider=none)");
    }
}
