package com.undercontroll.domain.model.insight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record InsightDraft(List<Item> insights) {

    public InsightDraft {
        insights = insights == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(insights));
    }

    public record Item(String candidateId, String text, String action) {
    }
}
