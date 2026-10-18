package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendChatMessageImplTest {

    private static final AnaChatSession SESSION = new AnaChatSession(
            new ConversationKey(7, "kitchen"),
            new AnaTurnRequest("kitchen", "Oi", "briefing", List.of(), false),
            null,
            true
    );

    @Mock
    private AnaTurnPreparer preparer;

    @Mock
    private AnaChatGateway gateway;

    @InjectMocks
    private SendChatMessageImpl useCase;

    @Test
    @DisplayName("replies through the prepared turn and records the outcome")
    void replies() {
        AnaReply reply = AnaReply.completed("Tem 2.");
        when(preparer.prepare("Oi", "kitchen")).thenReturn(SESSION);
        when(preparer.gateway()).thenReturn(gateway);
        when(gateway.reply(SESSION.turn())).thenReturn(reply);

        SendChatMessagePort.Output output = useCase.execute(new SendChatMessagePort.Input("Oi", "kitchen"));

        assertEquals("kitchen", output.conversationId());
        assertSame(reply, output.reply());
        verify(preparer).finish(SESSION, reply);
    }

    @Test
    @DisplayName("a failed reply still releases the conversation")
    void failureReleases() {
        when(preparer.prepare("Oi", "kitchen")).thenReturn(SESSION);
        when(preparer.gateway()).thenReturn(gateway);
        when(gateway.reply(SESSION.turn())).thenThrow(new IllegalStateException("down"));

        SendChatMessagePort.Input input = new SendChatMessagePort.Input("Oi", "kitchen");
        assertThrows(IllegalStateException.class, () -> useCase.execute(input));
        verify(preparer).finish(eq(SESSION), isNull());
    }
}
