package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.RequiredEvidence;
import nl.rubium.tegenspraak.chat.entity.CharacterMood;
import nl.rubium.tegenspraak.chat.entity.CharacterMoodRepository;
import nl.rubium.tegenspraak.chat.entity.ChatAttachment;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;
import nl.rubium.tegenspraak.investigation.control.EvidenceRef;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    public static final int MAX_ATTACHMENTS = 3;

    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);

    /** Guards against a model that keeps calling tools instead of answering. */
    private static final int MAX_TOOL_ROUNDS = 3;

    private final CharacterPromptBuilder characterPromptBuilder;
    private final ChatMessageRepository chatMessageRepository;
    private final CharacterMoodRepository characterMoodRepository;
    private final MessageToneClassifier messageToneClassifier;
    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final InvestigationService investigationService;
    private final ConversationLocks conversationLocks;
    private final int maxHistoryMessages;

    public ChatService(
            CharacterPromptBuilder characterPromptBuilder,
            ChatMessageRepository chatMessageRepository,
            CharacterMoodRepository characterMoodRepository,
            MessageToneClassifier messageToneClassifier,
            ChatModel chatModel,
            ToolCallingManager toolCallingManager,
            InvestigationService investigationService,
            ConversationLocks conversationLocks,
            @Value("${tegenspraak.chat.max-history-messages}") int maxHistoryMessages) {
        this.characterPromptBuilder = characterPromptBuilder;
        this.chatMessageRepository = chatMessageRepository;
        this.characterMoodRepository = characterMoodRepository;
        this.messageToneClassifier = messageToneClassifier;
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.investigationService = investigationService;
        this.conversationLocks = conversationLocks;
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public List<ConversationMessage> getConversation(UUID playerId, Conversation conversation) {
        investigationService.requireAvailableCharacter(
                conversation.teamId(), playerId, conversation.caseId(), conversation.characterId());
        TeamProgress progress =
                investigationService.getProgress(conversation.teamId(), playerId, conversation.caseId());
        return chatMessageRepository
                .findByTeamIdAndCaseIdAndCharacterIdOrderBySentAtAsc(
                        conversation.teamId(), conversation.caseId(), conversation.characterId())
                .stream()
                .map(message -> new ConversationMessage(message, resolveAttachments(message, progress)))
                .toList();
    }

    /**
     * Sends a team member's message to the character and returns the reply. Messages are only stored once the character
     * has replied, so a failed LLM call leaves the conversation untouched and the player can retry. Messages to the same
     * conversation are handled one at a time, see ConversationLocks.
     *
     * @param attachments mails and files the team has opened, shown to the character with the message
     */
    public ChatMessage sendMessage(
            UUID playerId,
            String playerName,
            Conversation conversation,
            String content,
            List<EvidenceRef> attachments) {
        List<EvidenceRef> distinctAttachments = attachments.stream().distinct().toList();
        if (distinctAttachments.size() > MAX_ATTACHMENTS) {
            throw new TooManyAttachmentsException(distinctAttachments.size());
        }
        CharacterProfile character = investigationService.requireAvailableCharacter(
                conversation.teamId(), playerId, conversation.caseId(), conversation.characterId());
        return conversationLocks.withLock(
                conversation,
                () -> replyAndStore(playerId, conversation, character, playerName, content, distinctAttachments));
    }

    /**
     * Progress is read inside the lock, so the character is only offered introductions that did not happen yet. Contacts
     * the character introduced are unlocked for the team and announced with a notice in this conversation. Evidence
     * shown with this message counts towards the character's secrets right away. The tone of the message changes the
     * character's mood before it replies, so the reply already reacts to it; the new mood is only stored with the reply.
     */
    private ChatMessage replyAndStore(
            UUID playerId,
            Conversation conversation,
            CharacterProfile character,
            String playerName,
            String content,
            List<EvidenceRef> attachments) {
        TeamProgress progress =
                investigationService.getProgress(conversation.teamId(), playerId, conversation.caseId());
        List<ChatAttachment> openAttachments = requireOpenAttachments(attachments, progress);
        List<ChatMessage> history = recentHistory(conversation);
        CharacterMood mood = currentMood(conversation, character);
        MessageTone tone =
                messageToneClassifier.classify(character, lastCharacterReply(history), content, openAttachments.size());
        mood.changeScore(tone.scoreChange());
        List<CharacterProfile> introducible = progress.introducibleBy(character).stream()
                .filter(contact -> character.mayIntroduce(contact.id(), mood.getMood()))
                .toList();
        IntroduceContactTool introduceContactTool = new IntroduceContactTool(introducible);

        ChatMessage playerMessage = ChatMessage.fromPlayer(
                conversation.teamId(),
                conversation.caseId(),
                conversation.characterId(),
                playerName,
                content,
                openAttachments);
        List<Message> prompt = new ArrayList<>();
        Set<RequiredEvidence> shownEvidence = shownEvidence(conversation, openAttachments);
        prompt.add(new SystemMessage(characterPromptBuilder.buildSystemPrompt(
                progress.caseDefinition(), character, introducible, shownEvidence, mood.getMood())));
        history.forEach(message -> prompt.add(toPromptMessage(message, progress)));
        prompt.add(toPromptMessage(playerMessage, progress));

        String reply = askModel(prompt, introducible.isEmpty() ? null : introduceContactTool, character.id());
        ChatMessage characterMessage = ChatMessage.fromCharacter(
                conversation.teamId(), conversation.caseId(), conversation.characterId(), reply);
        List<ChatMessage> newMessages = new ArrayList<>(List.of(playerMessage, characterMessage));
        investigationService
                .introduceCharacters(conversation.teamId(), conversation.caseId(), introduceContactTool.introduced())
                .forEach(contact -> newMessages.add(ChatMessage.notice(
                        conversation.teamId(),
                        conversation.caseId(),
                        conversation.characterId(),
                        character.name() + " heeft je in contact gebracht met " + contact.name() + ".")));
        chatMessageRepository.saveAll(newMessages);
        characterMoodRepository.save(mood);
        return characterMessage;
    }

    private CharacterMood currentMood(Conversation conversation, CharacterProfile character) {
        return characterMoodRepository
                .findByTeamIdAndCaseIdAndCharacterId(
                        conversation.teamId(), conversation.caseId(), conversation.characterId())
                .orElseGet(() -> CharacterMood.start(
                        conversation.teamId(),
                        conversation.caseId(),
                        conversation.characterId(),
                        character.startingMood()));
    }

    /** Null at the start of the conversation. */
    private static String lastCharacterReply(List<ChatMessage> history) {
        return history.reversed().stream()
                .filter(message -> message.getSender() == MessageSender.CHARACTER)
                .map(ChatMessage::getContent)
                .findFirst()
                .orElse(null);
    }

    /** Mails and files shown to the character earlier in this conversation or with the new message. */
    private Set<RequiredEvidence> shownEvidence(Conversation conversation, List<ChatAttachment> newAttachments) {
        List<ChatAttachment> earlierAttachments = chatMessageRepository.findAttachmentsInConversation(
                conversation.teamId(), conversation.caseId(), conversation.characterId());
        return Stream.concat(earlierAttachments.stream(), newAttachments.stream())
                .map(attachment ->
                        new RequiredEvidence(attachment.desktopId(), attachment.appId(), attachment.itemId()))
                .collect(Collectors.toSet());
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
        history.removeIf(message -> message.getSender() == MessageSender.NOTICE);
        while (!history.isEmpty() && history.getFirst().getSender() != MessageSender.PLAYER) {
            history.removeFirst();
        }
        return history;
    }

    /** Locked content must never reach the character, so every attachment has to be readable for the team. */
    private static List<ChatAttachment> requireOpenAttachments(List<EvidenceRef> attachments, TeamProgress progress) {
        return attachments.stream()
                .map(ref -> progress.findOpenEvidence(ref)
                        .orElseThrow(() -> new CaseContentNotFoundException("Attachment not available: " + ref)))
                .map(evidence -> new ChatAttachment(
                        evidence.desktop().id(),
                        evidence.app().id(),
                        evidence.item().id()))
                .toList();
    }

    /** Unlocks are never undone, so stored attachments stay readable; anything else is left out. */
    private static List<EvidenceItem> resolveAttachments(ChatMessage message, TeamProgress progress) {
        return message.getAttachments().stream()
                .map(attachment -> progress.findOpenEvidence(
                        new EvidenceRef(attachment.desktopId(), attachment.appId(), attachment.itemId())))
                .flatMap(Optional::stream)
                .toList();
    }

    /** Player messages carry the author's name, so the character can tell the team members apart. */
    private Message toPromptMessage(ChatMessage message, TeamProgress progress) {
        return switch (message.getSender()) {
            case PLAYER ->
                new UserMessage(characterPromptBuilder.formatPlayerMessage(
                        message.getAuthorName(), message.getContent(), resolveAttachments(message, progress)));
            case CHARACTER -> new AssistantMessage(message.getContent());
            case NOTICE -> throw new IllegalArgumentException("Notices are not part of the prompt");
        };
    }

    /** The tool is null when the character has nobody left to introduce. */
    private String askModel(List<Message> prompt, IntroduceContactTool introduceContactTool, String characterId) {
        ChatResponse response;
        try {
            response = introduceContactTool == null
                    ? chatModel.call(new Prompt(prompt))
                    : callWithTool(new Prompt(prompt, optionsWithTool(introduceContactTool)));
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
    private ChatResponse callWithTool(Prompt prompt) {
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
     * Runtime options replace the model's configured options instead of being merged with them, so the tool is added to
     * a copy of the configured options.
     */
    private ChatOptions optionsWithTool(IntroduceContactTool introduceContactTool) {
        if (!(chatModel.getOptions() instanceof ToolCallingChatOptions defaultOptions)) {
            throw new IllegalStateException("The chat model does not support tool calling");
        }
        return defaultOptions
                .mutate()
                .toolCallbacks(introduceContactTool.toCallback())
                .build();
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
