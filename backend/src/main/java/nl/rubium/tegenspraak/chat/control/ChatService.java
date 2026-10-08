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
import nl.rubium.tegenspraak.team.control.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
    private final TeamService teamService;
    private final ConversationLocks conversationLocks;
    private final int maxHistoryMessages;

    public ChatService(
            CaseCatalog caseCatalog,
            CharacterPromptBuilder characterPromptBuilder,
            ChatMessageRepository chatMessageRepository,
            ChatModel chatModel,
            TeamService teamService,
            ConversationLocks conversationLocks,
            @Value("${tegenspraak.chat.max-history-messages}") int maxHistoryMessages) {
        this.caseCatalog = caseCatalog;
        this.characterPromptBuilder = characterPromptBuilder;
        this.chatMessageRepository = chatMessageRepository;
        this.chatModel = chatModel;
        this.teamService = teamService;
        this.conversationLocks = conversationLocks;
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public List<ChatMessage> getConversation(UUID playerId, Conversation conversation) {
        teamService.requireMember(conversation.teamId(), playerId);
        caseCatalog.getCharacter(conversation.caseId(), conversation.characterId());
        return chatMessageRepository.findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtAsc(
                conversation.teamId(), conversation.caseId(), conversation.characterId());
    }

    /**
     * Sends a team member's message to the character and returns the reply. Both messages are only stored once the
     * character has replied, so a failed LLM call leaves the conversation untouched and the player can retry. Messages
     * to the same conversation are handled one at a time, see ConversationLocks.
     */
    public ChatMessage sendMessage(UUID playerId, String playerName, Conversation conversation, String content) {
        teamService.requireMember(conversation.teamId(), playerId);
        CaseDefinition caseDefinition = caseCatalog.getCase(conversation.caseId());
        CharacterProfile character = caseCatalog.getCharacter(conversation.caseId(), conversation.characterId());
        String systemPrompt = characterPromptBuilder.buildSystemPrompt(caseDefinition, character);

        return conversationLocks.withLock(
                conversation, () -> replyAndStore(conversation, systemPrompt, playerName, content));
    }

    private ChatMessage replyAndStore(
            Conversation conversation, String systemPrompt, String playerName, String content) {
        ChatMessage playerMessage = ChatMessage.fromPlayer(
                conversation.teamId(), conversation.caseId(), conversation.characterId(), playerName, content);
        List<Message> prompt = new ArrayList<>();
        prompt.add(new SystemMessage(systemPrompt));
        recentHistory(conversation).forEach(message -> prompt.add(toPromptMessage(message)));
        prompt.add(toPromptMessage(playerMessage));

        String reply = askModel(prompt, conversation.characterId());
        ChatMessage characterMessage = ChatMessage.fromCharacter(
                conversation.teamId(), conversation.caseId(), conversation.characterId(), reply);
        chatMessageRepository.saveAll(List.of(playerMessage, characterMessage));
        return characterMessage;
    }

    /** Latest messages in chronological order, starting with a player message as the LLM expects. */
    private List<ChatMessage> recentHistory(Conversation conversation) {
        List<ChatMessage> history =
                new ArrayList<>(chatMessageRepository.findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtDesc(
                        conversation.teamId(),
                        conversation.caseId(),
                        conversation.characterId(),
                        Limit.of(maxHistoryMessages)));
        Collections.reverse(history);
        while (!history.isEmpty() && history.getFirst().getSender() != MessageSender.PLAYER) {
            history.removeFirst();
        }
        return history;
    }

    /** Player messages carry the author's name, so the character can tell the team members apart. */
    private static Message toPromptMessage(ChatMessage message) {
        return switch (message.getSender()) {
            case PLAYER -> new UserMessage("[" + message.getAuthorName() + "]: " + message.getContent());
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
