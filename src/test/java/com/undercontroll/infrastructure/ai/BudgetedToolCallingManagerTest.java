package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetedToolCallingManagerTest {

    private final RecordingManager delegate = new RecordingManager();
    private final BudgetedToolCallingManager manager = new BudgetedToolCallingManager(delegate, new ObjectMapper(), 2, 1);

    @Test
    @DisplayName("within budget the delegate executes every call")
    void withinBudget() {
        ChatResponse response = toolCalls(call("1", "get_order"), call("2", "search_web"));

        ToolExecutionResult result = manager.executeToolCalls(new Prompt(List.of(new UserMessage("Oi"))), response);

        assertThat(delegate.executed).containsExactly(List.of("get_order", "search_web"));
        assertThat(responses(result)).extracting(ToolResponse::responseData).containsExactly("ran:get_order", "ran:search_web");
    }

    @Test
    @DisplayName("a second web search in the turn is refused while other calls still run, in call order")
    void webSearchLimit() {
        Prompt afterOneSearch = new Prompt(List.of(
                new UserMessage("Oi"),
                assistantWith(call("a", "search_web")),
                ToolResponseMessage.builder().responses(List.of(new ToolResponse("a", "search_web", "ok"))).build()
        ));

        ToolExecutionResult result = manager.executeToolCalls(afterOneSearch,
                toolCalls(call("3", "search_web"), call("4", "get_order")));

        assertThat(delegate.executed).containsExactly(List.of("get_order"));
        List<ToolResponse> responses = responses(result);
        assertThat(responses).extracting(ToolResponse::id).containsExactly("3", "4");
        assertThat(responses.get(0).responseData()).contains("invalid_input").contains("buscas na internet");
        assertThat(responses.get(1).responseData()).isEqualTo("ran:get_order");
        assertThat(result.returnDirect()).isFalse();
    }

    @Test
    @DisplayName("after the last allowed round every call is refused without running tools")
    void roundLimit() {
        Prompt twoRounds = new Prompt(List.of(
                new UserMessage("Oi"),
                assistantWith(call("a", "get_order")),
                ToolResponseMessage.builder().responses(List.of(new ToolResponse("a", "get_order", "ok"))).build(),
                assistantWith(call("b", "search_components")),
                ToolResponseMessage.builder().responses(List.of(new ToolResponse("b", "search_components", "ok"))).build()
        ));

        ToolExecutionResult result = manager.executeToolCalls(twoRounds, toolCalls(call("5", "get_order")));

        assertThat(delegate.executed).isEmpty();
        assertThat(responses(result).getFirst().responseData()).contains("Limite de consultas");
        List<Message> history = result.conversationHistory();
        assertThat(history).hasSize(7);
        assertThat(BudgetedToolCallingManager.rounds(new Prompt(history))).isEqualTo(3);
    }

    @Test
    @DisplayName("a model that ignores the refusal stops the turn")
    void beyondLimit() {
        List<Message> history = new ArrayList<>();
        history.add(new UserMessage("Oi"));
        for (int i = 0; i < 3; i++) {
            history.add(assistantWith(call("x" + i, "get_order")));
            history.add(ToolResponseMessage.builder().responses(List.of(new ToolResponse("x" + i, "get_order", "ok"))).build());
        }

        assertThatThrownBy(() -> manager.executeToolCalls(new Prompt(history), toolCalls(call("9", "get_order"))))
                .isInstanceOf(BudgetedToolCallingManager.ToolBudgetExceededException.class);
        assertThatThrownBy(() -> manager.executeToolCalls(new Prompt(history),
                new ChatResponse(List.of(new Generation(new AssistantMessage("texto"))))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("tool definitions come from the delegate")
    void resolvesDefinitions() {
        assertThat(manager.resolveToolDefinitions(ToolCallingChatOptions.builder().build())).isEmpty();
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, "function", name, "{}");
    }

    private static AssistantMessage assistantWith(ToolCall... calls) {
        return AssistantMessage.builder().content("").toolCalls(List.of(calls)).build();
    }

    private static ChatResponse toolCalls(ToolCall... calls) {
        return new ChatResponse(List.of(new Generation(assistantWith(calls))));
    }

    private static List<ToolResponse> responses(ToolExecutionResult result) {
        return ((ToolResponseMessage) result.conversationHistory().getLast()).getResponses();
    }

    private static final class RecordingManager implements ToolCallingManager {

        private final List<List<String>> executed = new ArrayList<>();

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            AssistantMessage assistant = chatResponse.getResult().getOutput();
            executed.add(assistant.getToolCalls().stream().map(ToolCall::name).toList());
            List<Message> history = new ArrayList<>(prompt.getInstructions());
            history.add(assistant);
            history.add(ToolResponseMessage.builder().responses(assistant.getToolCalls().stream()
                    .map(call -> new ToolResponse(call.id(), call.name(), "ran:" + call.name()))
                    .toList()).build());
            return ToolExecutionResult.builder().conversationHistory(history).build();
        }
    }
}
