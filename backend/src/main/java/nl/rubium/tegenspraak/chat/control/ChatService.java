package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);

    private final CaseCatalog caseCatalog;
    private final CharacterPromptBuilder characterPromptBuilder;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatModel chatModel;
    private final int maxHistoryMessages;

    public ChatService(
            CaseCatalog caseCatalog,
            CharacterPromptBuilder characterPromptBuilder,
            ChatMessageRepository chatMessageRepository,
            ChatModel chatModel,
            @Value("${tegenspraak.chat.max-history-messages}") int maxHistoryMessages) {
        this.caseCatalog = caseCatalog;
        this.characterPromptBuilder = characterPromptBuilder;
        this.chatMessageRepository = chatMessageRepository;
        this.chatModel = chatModel;
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public List<ChatMessage> getConversation(UUID playerId, String caseId, String characterId) {
        caseCatalog.getCharacter(caseId, characterId);
        return chatMessageRepository.findByPlayerIdAndCaseIdAndCharacterIdOrderBySentAtAsc(
                playerId, caseId, characterId);
    }

    /**
     * Sends the player's message to the character and returns the reply. Both messages are only stored once the
     * character has replied, so a failed LLM call leaves the conversation untouched and the player can retry.
     */
    public ChatMessage sendMessage(UUID playerId, String caseId, String characterId, String content) {
        CaseDefinition caseDefinition = caseCatalog.getCase(caseId);
        CharacterProfile character = caseCatalog.getCharacter(caseId, characterId);

        ChatMessage playerMessage = new ChatMessage(playerId, caseId, characterId, MessageSender.PLAYER, content);
        List<Message> prompt = new ArrayList<>();
        prompt.add(new SystemMessage(characterPromptBuilder.buildSystemPrompt(caseDefinition, character)));
        recentHistory(playerId, caseId, characterId).forEach(message -> prompt.add(toPromptMessage(message)));
        prompt.add(new UserMessage(content));

        String reply = askModel(prompt, characterId);
        ChatMessage characterMessage = new ChatMessage(playerId, caseId, characterId, MessageSender.CHARACTER, reply);
        chatMessageRepository.saveAll(List.of(playerMessage, characterMessage));
        return characterMessage;
    }

    /** Latest messages in chronological order, starting with a player message as the LLM expects. */
    private List<ChatMessage> recentHistory(UUID playerId, String caseId, String characterId) {
        List<ChatMessage> history =
                new ArrayList<>(chatMessageRepository.findByPlayerIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
                        playerId, caseId, characterId, Limit.of(maxHistoryMessages)));
        Collections.reverse(history);
        while (!history.isEmpty() && history.getFirst().getSender() != MessageSender.PLAYER) {
            history.removeFirst();
        }
        return history;
    }

    private static Message toPromptMessage(ChatMessage message) {
        return switch (message.getSender()) {
            case PLAYER -> new UserMessage(message.getContent());
            case CHARACTER -> new AssistantMessage(message.getContent());
        };
    }

    private String askModel(List<Message> prompt, String characterId) {
        ChatResponse response;
        try {
            response = chatModel.call(new Prompt(prompt));
        } catch (RuntimeException e) {
            LOG.warn("LLM call failed for character {}", characterId, e);
            throw new CharacterUnavailableException("LLM call failed for character " + characterId, e);
        }
        String reply = response == null || response.getResult() == null
                ? null
                : response.getResult().getOutput().getText();
        if (reply == null || reply.isBlank()) {
            throw new CharacterUnavailableException("Empty reply from character " + characterId);
        }
        return reply.strip();
    }
}
