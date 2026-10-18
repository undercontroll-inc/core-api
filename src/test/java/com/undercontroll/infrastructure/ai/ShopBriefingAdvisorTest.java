package com.undercontroll.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopBriefingAdvisorTest {

    @Mock
    private CallAdvisorChain chain;

    @Mock
    private StreamAdvisorChain streamChain;

    @Mock
    private ChatClientResponse response;

    @Test
    @DisplayName("inserts the shop briefing after existing system messages")
    void appendsBriefing() {
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(
                        new SystemMessage("persona"),
                        new UserMessage("Quais consertos estão abertos?")
                )),
                Map.of(ShopBriefingAdvisor.PARAM, "pedido 12, cliente Maria")
        );
        ArgumentCaptor<ChatClientRequest> forwarded = ArgumentCaptor.forClass(ChatClientRequest.class);
        when(chain.nextCall(forwarded.capture())).thenReturn(response);

        ShopBriefingAdvisor advisor = new ShopBriefingAdvisor();
        assertSame(response, advisor.adviseCall(request, chain));

        List<Message> messages = forwarded.getValue().prompt().getInstructions();
        assertEquals(3, messages.size());
        assertEquals("persona", messages.get(0).getText());
        assertInstanceOf(SystemMessage.class, messages.get(1));
        assertEquals(MessageType.SYSTEM, messages.get(1).getMessageType());
        assertTrue(messages.get(1).getText().contains("Maria"));
        assertEquals("Quais consertos estão abertos?", forwarded.getValue().prompt().getUserMessage().getText());
    }

    @Test
    @DisplayName("passes through when there is no briefing")
    void skipsBlank() {
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(new UserMessage("Oi")),
                Map.of()
        );
        when(chain.nextCall(request)).thenReturn(response);

        assertSame(response, new ShopBriefingAdvisor().adviseCall(request, chain));
        verify(chain).nextCall(request);
    }

    @Test
    @DisplayName("stream inserts the shop briefing after existing system messages")
    void appendsBriefingOnStream() {
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(
                        new SystemMessage("persona"),
                        new UserMessage("Quais consertos estão abertos?")
                )),
                Map.of(ShopBriefingAdvisor.PARAM, "pedido 12, cliente Maria")
        );
        ArgumentCaptor<ChatClientRequest> forwarded = ArgumentCaptor.forClass(ChatClientRequest.class);
        when(streamChain.nextStream(forwarded.capture())).thenReturn(Flux.just(response));

        List<ChatClientResponse> emitted = new ShopBriefingAdvisor()
                .adviseStream(request, streamChain)
                .collectList()
                .block();

        assertEquals(1, emitted.size());
        assertSame(response, emitted.getFirst());
        List<Message> messages = forwarded.getValue().prompt().getInstructions();
        assertEquals(3, messages.size());
        assertTrue(messages.get(1).getText().contains("Maria"));
    }
}
