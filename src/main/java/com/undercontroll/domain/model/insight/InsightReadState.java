package com.undercontroll.domain.model.insight;

public enum InsightReadState {
    READY,
    STALE,
    AWAITING_DATA,
    GENERATING,
    FAILED
}
