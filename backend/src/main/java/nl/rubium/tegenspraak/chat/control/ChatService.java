package nl.rubium.tegenspraak.chat.control;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.rubium.tegenspraak.casefile.control.CaseContentNotFoundException;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ConsultationUnlock;
import nl.rubium.tegenspraak.casefile.entity.ContactConsultation;
import nl.rubium.tegenspraak.casefile.entity.Mood;
import nl.rubium.tegenspraak.casefile.entity.RequiredEvidence;
import nl.rubium.tegenspraak.chat.entity.CharacterMood;
import nl.rubium.tegenspraak.chat.entity.CharacterMoodRepository;
import nl.rubium.tegenspraak.chat.entity.ChatAttachment;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationStatus;
import nl.rubium.tegenspraak.chat.entity.MessageSender;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.LockableItem;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;
import nl.rubium.tegenspraak.investigation.control.EvidenceRef;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import nl.rubium.tegenspraak.investigation.control.UnlockTarget;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ChatService {

    public static final int MAX_ATTACHMENTS = 3;

    private final CharacterPromptBuilder characterPromptBuilder;
    private final ChatMessageRepository chatMessageRepository;
    private final CharacterMoodRepository characterMoodRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final MessageToneClassifier messageToneClassifier;
    private final CharacterReplyModel characterReplyModel;
    private final InvestigationService investigationService;
    private final ConversationLocks conversationLocks;
    private final ConsultationSchedule consultationSchedule;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final int maxHistoryMessages;

    public ChatService(
            CharacterPromptBuilder characterPromptBuilder,
            ChatMessageRepository chatMessageRepository,
            CharacterMoodRepository characterMoodRepository,
            ConsultationRequestRepository consultationRequestRepository,
            MessageToneClassifier messageToneClassifier,
            CharacterReplyModel characterReplyModel,
            InvestigationService investigationService,
            ConversationLocks conversationLocks,
            ConsultationSchedule consultationSchedule,
            TransactionTemplate transactionTemplate,
            ApplicationEventPublisher eventPublisher,
            @Value("${tegenspraak.chat.max-history-messages}") int maxHistoryMessages) {
        this.characterPromptBuilder = characterPromptBuilder;
        this.chatMessageRepository = chatMessageRepository;
        this.characterMoodRepository = characterMoodRepository;
        this.consultationRequestRepository = consultationRequestRepository;
        this.messageToneClassifier = messageToneClassifier;
        this.characterReplyModel = characterReplyModel;
        this.investigationService = investigationService;
        this.conversationLocks = conversationLocks;
        this.consultationSchedule = consultationSchedule;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
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
     * Progress is read inside the lock, so the character is only offered introductions and consultations that did not
     * happen yet. Contacts the character introduced are unlocked for the team, and questions it asked other characters
     * are scheduled; both are announced with a notice in this conversation. Evidence shown with this message counts
     * towards the character's secrets right away. The tone of the message changes the character's mood before it
     * replies, so the reply already reacts to it; the new mood is only stored with the reply.
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
        ConsultationPrompt consultations =
                consultationPrompt(conversation, progress.caseDefinition(), character, mood.getMood());
        AskContactTool askContactTool = new AskContactTool(consultations.askable());

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
                progress.caseDefinition(), character, introducible, consultations, shownEvidence, mood.getMood())));
        history.forEach(message -> prompt.add(toPromptMessage(message, progress)));
        prompt.add(toPromptMessage(playerMessage, progress));

        List<ToolCallback> tools = new ArrayList<>();
        if (!introducible.isEmpty()) {
            tools.add(introduceContactTool.toCallback());
        }
        if (!consultations.askable().isEmpty()) {
            tools.add(askContactTool.toCallback());
        }
        String reply = characterReplyModel.reply(prompt, tools, character.id());
        ChatMessage characterMessage = ChatMessage.fromCharacter(
                conversation.teamId(), conversation.caseId(), conversation.characterId(), reply);
        transactionTemplate.executeWithoutResult(status -> {
            List<ChatMessage> newMessages = new ArrayList<>(List.of(playerMessage, characterMessage));
            List<CharacterProfile> introduced = investigationService.introduceCharacters(
                    conversation.teamId(), conversation.caseId(), introduceContactTool.introduced());
            introduced.forEach(contact -> newMessages.add(notice(
                    conversation, character.name() + " heeft je in contact gebracht met " + contact.name() + ".")));
            newMessages.addAll(scheduleQuestions(conversation, character, askContactTool.asked()));
            chatMessageRepository.saveAll(newMessages);
            characterMoodRepository.save(mood);
            publishChanges(conversation, !introduced.isEmpty());
        });
        return characterMessage;
    }

    /**
     * Lets the asking character pass a contact's answer on to the team, as a new message in its conversation. The
     * message, what the consultation unlocks and the answered request are stored together, so the answer is never
     * posted twice.
     *
     * @throws CharacterUnavailableException when the LLM fails; nothing is stored then, so it can be retried
     */
    public ChatMessage relayConsultationAnswer(
            ConsultationRequest request, ConsultationContact contact, CharacterProfile asker, String answer) {
        Conversation conversation = new Conversation(request.getTeamId(), request.getCaseId(), request.getAskerId());
        return conversationLocks.withLock(
                conversation, () -> relayAndStore(conversation, request, contact, asker, answer));
    }

    private ChatMessage relayAndStore(
            Conversation conversation,
            ConsultationRequest request,
            ConsultationContact contact,
            CharacterProfile asker,
            String answer) {
        TeamProgress progress = investigationService.getTeamProgress(conversation.teamId(), conversation.caseId());
        CharacterMood mood = currentMood(conversation, asker);
        List<Message> prompt = new ArrayList<>();
        prompt.add(new SystemMessage(characterPromptBuilder.buildSystemPrompt(
                progress.caseDefinition(),
                asker,
                List.of(),
                ConsultationPrompt.none(),
                shownEvidence(conversation, List.of()),
                mood.getMood())));
        recentHistory(conversation).forEach(message -> prompt.add(toPromptMessage(message, progress)));
        prompt.add(new UserMessage(
                characterPromptBuilder.formatContactAnswer(contact.contact().name(), request.getQuestion(), answer)));

        String reply = characterReplyModel.reply(prompt, List.of(), asker.id());
        ChatMessage characterMessage = ChatMessage.fromCharacter(
                conversation.teamId(), conversation.caseId(), conversation.characterId(), reply);
        transactionTemplate.executeWithoutResult(status -> {
            List<ChatMessage> newMessages = new ArrayList<>(List.of(characterMessage));
            for (ConsultationUnlock unlock : contact.consultation().unlocks()) {
                if (investigationService.unlockWithoutCode(
                        conversation.teamId(), conversation.caseId(), unlockTarget(unlock))) {
                    newMessages.add(notice(conversation, unlockNotice(progress.caseDefinition(), unlock)));
                }
            }
            chatMessageRepository.saveAll(newMessages);
            request.markAnswered();
            consultationRequestRepository.save(request);
            publishChanges(conversation, newMessages.size() > 1);
        });
        return characterMessage;
    }

    /**
     * Consultations the character may still start: not asked before by this team, and allowed by its mood. Pending
     * ones are listed too, so the character knows it is still waiting for an answer.
     */
    private ConsultationPrompt consultationPrompt(
            Conversation conversation, CaseDefinition caseDefinition, CharacterProfile character, Mood mood) {
        List<ConsultationRequest> requests = consultationRequestRepository.findByTeamIdAndCaseIdAndAskerId(
                conversation.teamId(), conversation.caseId(), conversation.characterId());
        Set<String> requestedIds =
                requests.stream().map(ConsultationRequest::getConsultationId).collect(Collectors.toSet());
        List<ConsultationContact> askable = character.consults().stream()
                .filter(consultation -> !requestedIds.contains(consultation.id()))
                .filter(consultation -> consultation.allows(mood))
                .map(consultation -> withContact(caseDefinition, consultation))
                .flatMap(Optional::stream)
                .toList();
        List<ConsultationContact> awaited = requests.stream()
                .filter(request -> request.getStatus() == ConsultationStatus.PENDING)
                .map(request -> character.findConsultation(request.getConsultationId()))
                .flatMap(Optional::stream)
                .map(consultation -> withContact(caseDefinition, consultation))
                .flatMap(Optional::stream)
                .toList();
        return new ConsultationPrompt(askable, awaited);
    }

    private static Optional<ConsultationContact> withContact(
            CaseDefinition caseDefinition, ContactConsultation consultation) {
        return caseDefinition
                .findCharacter(consultation.character())
                .map(contact -> new ConsultationContact(consultation, contact));
    }

    /** Stores a request per question, answered by the scheduler once it is due, and returns the notices for them. */
    private List<ChatMessage> scheduleQuestions(
            Conversation conversation, CharacterProfile character, List<AskContactTool.AskedQuestion> questions) {
        Instant now = consultationSchedule.now();
        List<ChatMessage> notices = new ArrayList<>();
        for (AskContactTool.AskedQuestion question : questions) {
            consultationRequestRepository.save(new ConsultationRequest(
                    conversation.teamId(),
                    conversation.caseId(),
                    conversation.characterId(),
                    question.contact().consultation().id(),
                    question.question(),
                    now,
                    consultationSchedule.replyDueAt(now)));
            notices.add(notice(
                    conversation,
                    character.name() + " heeft " + question.contact().contact().name() + " een bericht gestuurd."));
        }
        return notices;
    }

    /** Pushed to the team once the transaction commits, see TeamEventStreams. */
    private void publishChanges(Conversation conversation, boolean unlockedSomething) {
        eventPublisher.publishEvent(
                TeamEvent.chat(conversation.teamId(), conversation.caseId(), conversation.characterId()));
        if (unlockedSomething) {
            eventPublisher.publishEvent(TeamEvent.caseProgress(conversation.teamId(), conversation.caseId()));
        }
    }

    private static UnlockTarget unlockTarget(ConsultationUnlock unlock) {
        if (unlock.isCharacter()) {
            return UnlockTarget.character(unlock.character());
        }
        if (unlock.isItem()) {
            return UnlockTarget.item(unlock.desktop(), unlock.app(), unlock.item());
        }
        return UnlockTarget.desktop(unlock.desktop());
    }

    /** The case was validated at startup, so everything an unlock points to exists. */
    private static String unlockNotice(CaseDefinition caseDefinition, ConsultationUnlock unlock) {
        if (unlock.isCharacter()) {
            CharacterProfile contact =
                    caseDefinition.findCharacter(unlock.character()).orElseThrow();
            return "Je kunt nu contact opnemen met " + contact.name() + ".";
        }
        Desktop desktop = caseDefinition.findDesktop(unlock.desktop()).orElseThrow();
        if (!unlock.isItem()) {
            return "De computer van " + desktop.ownerName() + " is ontgrendeld.";
        }
        DesktopApp app = desktop.findApp(unlock.app()).orElseThrow();
        LockableItem item = app.findItem(unlock.item()).orElseThrow();
        return "\"" + new EvidenceItem(desktop, app, item).title() + "\" op de computer van " + desktop.ownerName()
                + " is ontgrendeld.";
    }

    private static ChatMessage notice(Conversation conversation, String content) {
        return ChatMessage.notice(conversation.teamId(), conversation.caseId(), conversation.characterId(), content);
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
}
