package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.infrastructure.metrics.FeatureMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BudgetedToolCallingManager implements ToolCallingManager {

    static final String WEB_SEARCH = "search_web";
    static final String ROUND_LIMIT = "Limite de consultas deste turno atingido. Responda agora com o que já tem.";
    static final String WEB_LIMIT = "Limite de buscas na internet deste turno atingido. Use o que já encontrou.";

    private final ToolCallingManager delegate;
    private final ObjectMapper objectMapper;
    private final int maxRounds;
    private final int maxWebSearches;
    private final FeatureMetrics metrics;

    public BudgetedToolCallingManager(
            ToolCallingManager delegate,
            ObjectMapper objectMapper,
            int maxRounds,
            int maxWebSearches,
            FeatureMetrics metrics
    ) {
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.maxRounds = maxRounds;
        this.maxWebSearches = maxWebSearches;
        this.metrics = metrics;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        Generation generation = chatResponse.getResults().stream()
                .filter(result -> result.getOutput().hasToolCalls())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tool call requested by the chat model"));
        AssistantMessage assistant = generation.getOutput();
        int rounds = rounds(prompt);
        if (rounds > maxRounds) {
            assistant.getToolCalls().forEach(call -> metrics.tool(call.name(), "stopped"));
            throw new ToolBudgetExceededException(rounds);
        }
        int webSearches = webSearches(prompt);
        List<ToolCall> allowed = new ArrayList<>();
        Map<String, String> refused = new HashMap<>();
        for (ToolCall call : assistant.getToolCalls()) {
            if (rounds == maxRounds) {
                refused.put(call.id(), ROUND_LIMIT);
            } else if (WEB_SEARCH.equals(call.name()) && webSearches >= maxWebSearches) {
                refused.put(call.id(), WEB_LIMIT);
            } else {
                if (WEB_SEARCH.equals(call.name())) {
                    webSearches++;
                }
                allowed.add(call);
            }
        }
        allowed.forEach(call -> metrics.tool(call.name(), "executed"));
        assistant.getToolCalls().stream()
                .filter(call -> refused.containsKey(call.id()))
                .forEach(call -> metrics.tool(call.name(), "refused"));
        if (refused.isEmpty()) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }
        log.info("Ana tool budget refused calls={} round={}", refused.size(), rounds + 1);
        Map<String, ToolResponse> executed = new HashMap<>();
        if (!allowed.isEmpty()) {
            executeAllowed(prompt, chatResponse, generation, assistant, allowed)
                    .forEach(response -> executed.put(response.id(), response));
        }
        List<ToolResponse> responses = assistant.getToolCalls().stream()
                .map(call -> refused.containsKey(call.id())
                        ? new ToolResponse(call.id(), call.name(), refusal(refused.get(call.id())))
                        : executed.get(call.id()))
                .toList();
        List<Message> history = new ArrayList<>(prompt.getInstructions());
        history.add(assistant);
        history.add(ToolResponseMessage.builder().responses(responses).build());
        return ToolExecutionResult.builder().conversationHistory(history).returnDirect(false).build();
    }

    private List<ToolResponse> executeAllowed(
            Prompt prompt,
            ChatResponse chatResponse,
            Generation generation,
            AssistantMessage assistant,
            List<ToolCall> allowed
    ) {
        AssistantMessage reduced = AssistantMessage.builder()
                .content(assistant.getText())
                .properties(assistant.getMetadata())
                .toolCalls(allowed)
                .build();
        ChatResponse reducedResponse = new ChatResponse(
                List.of(new Generation(reduced, generation.getMetadata())),
                chatResponse.getMetadata()
        );
        List<Message> history = delegate.executeToolCalls(prompt, reducedResponse).conversationHistory();
        return ((ToolResponseMessage) history.getLast()).getResponses();
    }

    static int rounds(Prompt prompt) {
        return (int) prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance).count();
    }

    static int webSearches(Prompt prompt) {
        return (int) prompt.getInstructions().stream()
                .filter(AssistantMessage.class::isInstance)
                .map(AssistantMessage.class::cast)
                .flatMap(message -> message.getToolCalls().stream())
                .filter(call -> WEB_SEARCH.equals(call.name()))
                .count();
    }

    private String refusal(String message) {
        try {
            return objectMapper.writeValueAsString(ToolResult.invalidInput(message));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static final class ToolBudgetExceededException extends RuntimeException {

        ToolBudgetExceededException(int rounds) {
            super("Ana kept calling tools after " + rounds + " rounds");
        }
    }
}
