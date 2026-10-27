package nl.rubium.tegenspraak.chat.control;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/** Lets the LLM write a character's reply, running the character's tools until it answers with text. */
@Component
public class CharacterReplyModel {

    private static final Logger LOG = LoggerFactory.getLogger(CharacterReplyModel.class);

    /** Guards against a model that keeps calling tools instead of answering. */
    private static final int MAX_TOOL_ROUNDS = 3;

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;

    public CharacterReplyModel(ChatModel chatModel, ToolCallingManager toolCallingManager) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
    }

    /**
     * @param tools empty when the character has nothing to do but answer
     * @throws CharacterUnavailableException when the LLM fails or gives no text
     */
    public String reply(List<Message> messages, List<ToolCallback> tools, String characterId) {
        ChatResponse response;
        try {
            response = tools.isEmpty()
                    ? chatModel.call(new Prompt(messages))
                    : callWithTools(new Prompt(messages, optionsWithTools(tools)));
        } catch (RuntimeException e) {
            LOG.warn("LLM call failed for character {}", characterId, e);
            throw new CharacterUnavailableException("LLM call failed for character " + characterId, e);
        }
        Generation replyGeneration = finalGeneration(response);
        String reply =
                replyGeneration == null ? null : replyGeneration.getOutput().getText();
        if (reply == null || reply.isBlank()) {
            String finishReason = replyGeneration == null
                    ? "none"
                    : replyGeneration.getMetadata().getFinishReason();
            LOG.warn("Empty reply from character {}, finish reason: {}", characterId, finishReason);
            throw new CharacterUnavailableException("Empty reply from character " + characterId);
        }
        return reply.strip();
    }

    /**
     * The chat model only returns the tool call, it does not execute it. The tool runs here and its result goes back to
     * the model, until the model answers with text.
     */
    private ChatResponse callWithTools(Prompt prompt) {
        Prompt currentPrompt = prompt;
        ChatResponse response = chatModel.call(currentPrompt);
        for (int round = 0; round < MAX_TOOL_ROUNDS && response != null && response.hasToolCalls(); round++) {
            List<Message> history =
                    toolCallingManager.executeToolCalls(currentPrompt, response).conversationHistory();
            currentPrompt = new Prompt(history, prompt.getOptions());
            response = chatModel.call(currentPrompt);
        }
        return response;
    }

    /**
     * Runtime options replace the model's configured options instead of being merged with them, so the tools are added
     * to a copy of the configured options.
     */
    private ChatOptions optionsWithTools(List<ToolCallback> tools) {
        if (!(chatModel.getOptions() instanceof ToolCallingChatOptions defaultOptions)) {
            throw new IllegalStateException("The chat model does not support tool calling");
        }
        return defaultOptions.mutate().toolCallbacks(tools).build();
    }

    /**
     * Thinking blocks come back as separate generations before the answer, so the text reply is the last generation,
     * not the first.
     */
    private static Generation finalGeneration(ChatResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            return null;
        }
        return response.getResults().getLast();
    }
}
