package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.insight.InsightDraft;

public interface InsightDraftCodec {

    String encode(InsightDraft draft);

    InsightDraft decode(String json);
}
