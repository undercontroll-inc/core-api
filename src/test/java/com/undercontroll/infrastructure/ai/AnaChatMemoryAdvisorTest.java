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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaChatMemoryAdvisorTest {

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private CallAdvisorChain chain;

    @Mock
    private StreamAdvisorChain streamChain;

    @Mock
    private ChatClientResponse response;

    @Test
    @DisplayName("merges system messages into one, then history and the new user message")
    void ordersMessages() {
        when(chatMemory.get("7")).thenReturn(List.of(
                new UserMessage("antes"),
                new AssistantMessage("ok")
        ));
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(
                        new SystemMessage("persona"),
                        new SystemMessage("briefing"),
                        new UserMessage("agora")
                )),
                Map.of(ChatMemory.CONVERSATION_ID, "7")
        );
        ArgumentCaptor<ChatClientRequest> forwarded = ArgumentCaptor.forClass(ChatClientRequest.class);
        when(chain.nextCall(forwarded.capture())).thenReturn(response);
        when(response.chatResponse()).thenReturn(null);

        AnaChatMemoryAdvisor advisor = new AnaChatMemoryAdvisor(chatMemory);
        assertSame(response, advisor.adviseCall(request, chain));

        List<Message> messages = forwarded.getValue().prompt().getInstructions();
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals("persona\n\nbriefing", messages.get(0).getText());
        assertEquals("antes", messages.get(1).getText());
        assertEquals("ok", messages.get(2).getText());
        assertEquals("agora", messages.get(3).getText());
        verify(chatMemory).add(eq("7"), any(UserMessage.class));
    }

    @Test
    @DisplayName("does not persist the user message when the model call fails")
    void skipsMemoryWhenCallFails() {
        when(chatMemory.get("7")).thenReturn(List.of());
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(new UserMessage("agora"))),
                Map.of(ChatMemory.CONVERSATION_ID, "7")
        );
        when(chain.nextCall(any())).thenThrow(new IllegalStateException("model down"));

        AnaChatMemoryAdvisor advisor = new AnaChatMemoryAdvisor(chatMemory);
        assertThrows(IllegalStateException.class, () -> advisor.adviseCall(request, chain));
        verify(chatMemory, never()).add(any(), any(Message.class));
    }

    @Test
    @DisplayName("stream injects history and persists memory only after the stream completes")
    void persistsOnStreamComplete() {
        when(chatMemory.get("7")).thenReturn(List.of(
                new UserMessage("antes"),
                new AssistantMessage("ok")
        ));
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(
                        new SystemMessage("persona"),
                        new UserMessage("agora")
                )),
                Map.of(ChatMemory.CONVERSATION_ID, "7")
        );
        ArgumentCaptor<ChatClientRequest> forwarded = ArgumentCaptor.forClass(ChatClientRequest.class);
        ChatClientResponse chunk = ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("final")))))
                .context(request.context())
                .build();
        when(streamChain.nextStream(forwarded.capture())).thenReturn(Flux.just(chunk));

        List<ChatClientResponse> emitted = new AnaChatMemoryAdvisor(chatMemory)
                .adviseStream(request, streamChain)
                .collectList()
                .block();

        assertEquals(1, emitted.size());
        List<Message> messages = forwarded.getValue().prompt().getInstructions();
        assertEquals(MessageType.SYSTEM, messages.get(0).getMessageType());
        assertEquals("persona", messages.get(0).getText());
        assertEquals("antes", messages.get(1).getText());
        assertEquals("ok", messages.get(2).getText());
        assertEquals("agora", messages.get(3).getText());
        verify(chatMemory).add(eq("7"), any(UserMessage.class));
        verify(chatMemory).add(eq("7"), any(List.class));
    }

    @Test
    @DisplayName("does not persist memory when the stream is cancelled")
    void skipsMemoryWhenStreamCancelled() throws InterruptedException {
        when(chatMemory.get("7")).thenReturn(List.of());
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(new UserMessage("agora"))),
                Map.of(ChatMemory.CONVERSATION_ID, "7")
        );
        CountDownLatch subscribed = new CountDownLatch(1);
        when(streamChain.nextStream(any())).thenAnswer(invocation -> {
            subscribed.countDown();
            return Flux.never();
        });

        Disposable subscription = new AnaChatMemoryAdvisor(chatMemory)
                .adviseStream(request, streamChain)
                .subscribe();
        assertTrue(subscribed.await(2, TimeUnit.SECONDS));
        subscription.dispose();

        verify(chatMemory, never()).add(any(), any(Message.class));
        verify(chatMemory, never()).add(any(), any(List.class));
    }

    @Test
    @DisplayName("does not persist memory when the stream fails")
    void skipsMemoryWhenStreamFails() {
        when(chatMemory.get("7")).thenReturn(List.of());
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(List.of(new UserMessage("agora"))),
                Map.of(ChatMemory.CONVERSATION_ID, "7")
        );
        when(streamChain.nextStream(any())).thenReturn(Flux.error(new IllegalStateException("model down")));

        assertThrows(IllegalStateException.class, () -> new AnaChatMemoryAdvisor(chatMemory)
                .adviseStream(request, streamChain)
                .blockLast());
        verify(chatMemory, never()).add(any(), any(Message.class));
        verify(chatMemory, never()).add(any(), any(List.class));
    }
}
