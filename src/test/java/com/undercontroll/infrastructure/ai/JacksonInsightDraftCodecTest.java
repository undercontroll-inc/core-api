package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.model.insight.InsightDraft;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacksonInsightDraftCodecTest {

    private final JacksonInsightDraftCodec codec = new JacksonInsightDraftCodec(new ObjectMapper());

    @Test
    @DisplayName("stores the draft in the same JSON shape the model returns")
    void roundTrip() {
        InsightDraft draft = new InsightDraft(List.of(new InsightDraft.Item("A:1", "{{assunto}} subiu.", "Estocar.")));

        String json = codec.encode(draft);

        assertEquals("{\"insights\":[{\"candidate_id\":\"A:1\",\"text\":\"{{assunto}} subiu.\",\"action\":\"Estocar.\"}]}", json);
        assertEquals(draft, codec.decode(json));
        assertEquals(new InsightDraft(List.of()), codec.decode("{}"));
        assertThrows(IllegalStateException.class, () -> codec.decode("{"));
    }
}
