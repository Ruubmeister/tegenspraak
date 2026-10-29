package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ConsultationUnlock;
import nl.rubium.tegenspraak.chat.entity.CharacterMood;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.DesktopApp;
import nl.rubium.tegenspraak.desktop.entity.LockableItem;
import nl.rubium.tegenspraak.investigation.control.EvidenceItem;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import nl.rubium.tegenspraak.investigation.control.UnlockTarget;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lets the asking character pass a contact's answer on to the team, as a new message in its conversation, and opens
 * what the consultation unlocks. The message, the unlocks and the answered request are stored together, so the answer
 * is never posted twice.
 */
@Component
class ConsultationRelay {

    private final CharacterPromptBuilder characterPromptBuilder;
    private final CharacterReplyModel characterReplyModel;
    private final ConversationMemory conversationMemory;
    private final ConversationLocks conversationLocks;
    private final ChatMessageRepository chatMessageRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final InvestigationService investigationService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    ConsultationRelay(
            CharacterPromptBuilder characterPromptBuilder,
            CharacterReplyModel characterReplyModel,
            ConversationMemory conversationMemory,
            ConversationLocks conversationLocks,
            ChatMessageRepository chatMessageRepository,
            ConsultationRequestRepository consultationRequestRepository,
            InvestigationService investigationService,
            TransactionTemplate transactionTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.characterPromptBuilder = characterPromptBuilder;
        this.characterReplyModel = characterReplyModel;
        this.conversationMemory = conversationMemory;
        this.conversationLocks = conversationLocks;
        this.chatMessageRepository = chatMessageRepository;
        this.consultationRequestRepository = consultationRequestRepository;
        this.investigationService = investigationService;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Messages to the same conversation are handled one at a time, see ConversationLocks.
     *
     * @throws CharacterUnavailableException when the LLM fails; nothing is stored then, so it can be retried
     */
    ChatMessage relay(ConsultationRequest request, ConsultationContact contact, CharacterProfile asker, String answer) {
        Conversation conversation = new Conversation(request.getTeamId(), request.getCaseId(), request.getAskerId());
        return conversationLocks.withLock(
                conversation, () -> relayAndStore(conversation, request, contact, asker, answer));
    }

    /** The answer is shown to the asker as its latest message, so it writes a new chat message that passes it on. */
    private ChatMessage relayAndStore(
            Conversation conversation,
            ConsultationRequest request,
            ConsultationContact contact,
            CharacterProfile asker,
            String answer) {
        TeamProgress progress = investigationService.getTeamProgress(conversation.teamId(), conversation.caseId());
        CharacterMood mood = conversationMemory.currentMood(conversation, asker);
        String systemPrompt = characterPromptBuilder.buildSystemPrompt(
                progress.caseDefinition(),
                asker,
                List.of(),
                ConsultationPrompt.none(),
                conversationMemory.shownEvidence(conversation, List.of()),
                mood.getMood());
        List<Message> prompt =
                conversationMemory.prompt(systemPrompt, conversationMemory.recentHistory(conversation), progress);
        prompt.add(new UserMessage(
                characterPromptBuilder.formatContactAnswer(contact.contact().name(), request.getQuestion(), answer)));

        String reply = characterReplyModel.reply(prompt, List.of(), asker.id());
        ChatMessage characterMessage = conversation.characterMessage(reply);
        transactionTemplate.executeWithoutResult(status -> {
            List<ChatMessage> unlockNotices = unlock(conversation, progress.caseDefinition(), contact);
            List<ChatMessage> newMessages = new ArrayList<>(List.of(characterMessage));
            newMessages.addAll(unlockNotices);
            chatMessageRepository.saveAll(newMessages);
            request.markAnswered();
            consultationRequestRepository.save(request);
            publishChanges(conversation, !unlockNotices.isEmpty());
        });
        return characterMessage;
    }

    /** Returns a notice for everything that was newly unlocked; what the team already had is skipped. */
    private List<ChatMessage> unlock(
            Conversation conversation, CaseDefinition caseDefinition, ConsultationContact contact) {
        List<ChatMessage> notices = new ArrayList<>();
        for (ConsultationUnlock unlock : contact.consultation().unlocks()) {
            if (investigationService.unlockWithoutCode(
                    conversation.teamId(), conversation.caseId(), UnlockTarget.of(unlock))) {
                notices.add(conversation.notice(unlockNotice(caseDefinition, unlock)));
            }
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
}
