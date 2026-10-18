package com.undercontroll.domain.exception;

public class InsightLeaseLostException extends RuntimeException {

    public InsightLeaseLostException(long jobId) {
        super("Lease on insight job " + jobId + " is no longer held by this worker");
    }
}
