package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.Mood;
import nl.rubium.tegenspraak.chat.entity.CharacterMood;
import nl.rubium.tegenspraak.chat.entity.CharacterMoodRepository;
import nl.rubium.tegenspraak.chat.entity.ChatAttachment;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.desktop.entity.ItemRef;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** The team's conversations with the characters of a case: reading them, and sending a message for a reply. */
@Service
public class ChatService {

    public static final int MAX_ATTACHMENTS = 3;

    private final CharacterPromptBuilder characterPromptBuilder;
    private final ConversationMemory conversationMemory;
    private final ConsultationRequests consultationRequests;
    private final ChatMessageRepository chatMessageRepository;
    private final CharacterMoodRepository characterMoodRepository;
    private final MessageToneClassifier messageToneClassifier;
    private final CharacterReplyModel characterReplyModel;
    private final InvestigationService investigationService;
    private final ConversationLocks conversationLocks;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public ChatService(
            CharacterPromptBuilder characterPromptBuilder,
            ConversationMemory conversationMemory,
            ConsultationRequests consultationRequests,
            ChatMessageRepository chatMessageRepository,
            CharacterMoodRepository characterMoodRepository,
            MessageToneClassifier messageToneClassifier,
            CharacterReplyModel characterReplyModel,
            InvestigationService investigationService,
            ConversationLocks conversationLocks,
            TransactionTemplate transactionTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.characterPromptBuilder = characterPromptBuilder;
        this.conversationMemory = conversationMemory;
        this.consultationRequests = consultationRequests;
        this.chatMessageRepository = chatMessageRepository;
        this.characterMoodRepository = characterMoodRepository;
        this.messageToneClassifier = messageToneClassifier;
        this.characterReplyModel = characterReplyModel;
        this.investigationService = investigationService;
        this.conversationLocks = conversationLocks;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
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
                .map(message ->
                        new ConversationMessage(message, ConversationMemory.resolveAttachments(message, progress)))
                .toList();
    }

    /**
     * Sends a team member's message to the character and returns the reply. Messages are only stored once the character
     * has replied, so a failed LLM call leaves the conversation untouched and the player can retry. Messages to the same
     * conversation are handled one at a time, see ConversationLocks.
     *
     * @param attachments desktop items the team has opened, shown to the character with the message
     */
    public ChatMessage sendMessage(
            UUID playerId, String playerName, Conversation conversation, String content, List<ItemRef> attachments) {
        List<ItemRef> distinctAttachments = attachments.stream().distinct().toList();
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
     * Progress is read inside the lock, so the character is only offered introductions and consultations that did not
     * happen yet. Evidence shown with this message counts towards the character's secrets right away. The tone of the
     * message changes the character's mood before it replies, so the reply already reacts to it; the new mood is only
     * stored with the reply.
     */
    private ChatMessage replyAndStore(
            UUID playerId,
            Conversation conversation,
            CharacterProfile character,
            String playerName,
            String content,
            List<ItemRef> attachments) {
        TeamProgress progress =
                investigationService.getProgress(conversation.teamId(), playerId, conversation.caseId());
        List<ChatAttachment> openAttachments = requireOpenAttachments(attachments, progress);
        List<ChatMessage> history = conversationMemory.recentHistory(conversation);
        CharacterMood mood = conversationMemory.currentMood(conversation, character);
        MessageTone tone =
                messageToneClassifier.classify(character, lastCharacterReply(history), content, openAttachments.size());
        mood.changeScore(tone.scoreChange());

        List<CharacterProfile> introducible = introducibleContacts(progress, character, mood.getMood());
        ConsultationPrompt consultations =
                consultationRequests.forCharacter(conversation, progress.caseDefinition(), character, mood.getMood());
        IntroduceContactTool introduceContactTool = new IntroduceContactTool(introducible);
        AskContactTool askContactTool = new AskContactTool(consultations.askable());

        ChatMessage playerMessage = ChatMessage.fromPlayer(
                conversation.teamId(),
                conversation.caseId(),
                conversation.characterId(),
                playerName,
                content,
                openAttachments);
        String systemPrompt = characterPromptBuilder.buildSystemPrompt(
                progress.caseDefinition(),
                character,
                introducible,
                consultations,
                conversationMemory.shownEvidence(conversation, openAttachments),
                mood.getMood());
        List<Message> prompt = conversationMemory.prompt(systemPrompt, history, progress);
        prompt.add(conversationMemory.toPromptMessage(playerMessage, progress));

        String reply =
                characterReplyModel.reply(prompt, offeredTools(introduceContactTool, askContactTool), character.id());
        ChatMessage characterMessage = conversation.characterMessage(reply);
        transactionTemplate.executeWithoutResult(status -> storeExchange(
                conversation,
                character,
                mood,
                List.of(playerMessage, characterMessage),
                introduceContactTool.introduced(),
                askContactTool.asked()));
        return characterMessage;
    }

    /**
     * Contacts the character introduced are unlocked for the team, and questions it asked other characters are
     * scheduled; both are announced with a notice in this conversation.
     */
    private void storeExchange(
            Conversation conversation,
            CharacterProfile character,
            CharacterMood mood,
            List<ChatMessage> exchange,
            List<CharacterProfile> introducedByTool,
            List<AskContactTool.AskedQuestion> askedQuestions) {
        List<ChatMessage> newMessages = new ArrayList<>(exchange);
        List<CharacterProfile> introduced = investigationService.introduceCharacters(
                conversation.teamId(), conversation.caseId(), introducedByTool);
        introduced.forEach(contact -> newMessages.add(
                conversation.notice(character.name() + " heeft je in contact gebracht met " + contact.name() + ".")));
        newMessages.addAll(consultationRequests.schedule(conversation, character, askedQuestions));
        chatMessageRepository.saveAll(newMessages);
        characterMoodRepository.save(mood);
        publishChanges(conversation, !introduced.isEmpty());
    }

    /** Hidden characters this character may still introduce, as far as its mood allows. */
    private static List<CharacterProfile> introducibleContacts(
            TeamProgress progress, CharacterProfile character, Mood mood) {
        return progress.introducibleBy(character).stream()
                .filter(contact -> character.mayIntroduce(contact.id(), mood))
                .toList();
    }

    /** Tools are only offered when the character has someone to introduce or ask. */
    private static List<ToolCallback> offeredTools(
            IntroduceContactTool introduceContactTool, AskContactTool askContactTool) {
        List<ToolCallback> tools = new ArrayList<>();
        if (introduceContactTool.canIntroduce()) {
            tools.add(introduceContactTool.toCallback());
        }
        if (askContactTool.canAsk()) {
            tools.add(askContactTool.toCallback());
        }
        return tools;
    }

    /** Pushed to the team once the transaction commits, see TeamEventStreams. */
    private void publishChanges(Conversation conversation, boolean unlockedSomething) {
        eventPublisher.publishEvent(
                TeamEvent.chat(conversation.teamId(), conversation.caseId(), conversation.characterId()));
        if (unlockedSomething) {
            eventPublisher.publishEvent(TeamEvent.caseProgress(conversation.teamId(), conversation.caseId()));
        }
    }

    /** Null at the start of the conversation. */
    private static String lastCharacterReply(List<ChatMessage> history) {
        return history.reversed().stream()
                .filter(message -> message.getSender() == MessageSender.CHARACTER)
                .map(ChatMessage::getContent)
                .findFirst()
                .orElse(null);
    }

    /** Locked content must never reach the character, so every attachment has to be readable for the team. */
    private static List<ChatAttachment> requireOpenAttachments(List<ItemRef> attachments, TeamProgress progress) {
        for (ItemRef ref : attachments) {
            if (progress.findOpenEvidence(ref).isEmpty()) {
                throw new CaseContentNotFoundException("Attachment not available: " + ref);
            }
        }
        return attachments.stream().map(ChatAttachment::of).toList();
    }
}
