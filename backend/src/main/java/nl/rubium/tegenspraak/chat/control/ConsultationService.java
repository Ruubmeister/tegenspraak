package nl.rubium.tegenspraak.chat.control;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ChatMessageRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationStatus;
import nl.rubium.tegenspraak.liveupdate.entity.TeamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Answers the questions characters asked each other once they are due: the contact's LLM writes the answer, then the
 * asking character passes it on to the team. A failed answer is retried later; after the last attempt the request is
 * dropped with a notice, so the character can ask again.
 */
@Service
public class ConsultationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultationService.class);

    private final ConsultationRequestRepository consultationRequestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CaseCatalog caseCatalog;
    private final CharacterPromptBuilder characterPromptBuilder;
    private final CharacterReplyModel characterReplyModel;
    private final ConsultationRelay consultationRelay;
    private final ConsultationSchedule consultationSchedule;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public ConsultationService(
            ConsultationRequestRepository consultationRequestRepository,
            ChatMessageRepository chatMessageRepository,
            CaseCatalog caseCatalog,
            CharacterPromptBuilder characterPromptBuilder,
            CharacterReplyModel characterReplyModel,
            ConsultationRelay consultationRelay,
            ConsultationSchedule consultationSchedule,
            TransactionTemplate transactionTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.consultationRequestRepository = consultationRequestRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.caseCatalog = caseCatalog;
        this.characterPromptBuilder = characterPromptBuilder;
        this.characterReplyModel = characterReplyModel;
        this.consultationRelay = consultationRelay;
        this.consultationSchedule = consultationSchedule;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    /** Each request is handled on its own, so one that fails does not hold up the others. */
    public void answerDueConsultations() {
        List<ConsultationRequest> due = consultationRequestRepository.findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                ConsultationStatus.PENDING, consultationSchedule.now());
        for (ConsultationRequest request : due) {
            try {
                answer(request);
            } catch (RuntimeException e) {
                LOG.error("Could not answer consultation request {}", request.getId(), e);
            }
        }
    }

    private void answer(ConsultationRequest request) {
        Optional<ResolvedRequest> resolved = resolve(request);
        if (resolved.isEmpty()) {
            LOG.warn(
                    "Dropping consultation request {}: consultation '{}' of '{}' is no longer in case '{}'",
                    request.getId(),
                    request.getConsultationId(),
                    request.getAskerId(),
                    request.getCaseId());
            consultationRequestRepository.delete(request);
            return;
        }
        ResolvedRequest consultation = resolved.get();
        try {
            String answer = contactAnswer(consultation, request.getQuestion());
            consultationRelay.relay(request, consultation.contact(), consultation.asker(), answer);
        } catch (CharacterUnavailableException e) {
            retryOrGiveUp(request, consultation);
        }
    }

    /** Cases are loaded at startup, so a request stored before the case files changed may point to nothing. */
    private Optional<ResolvedRequest> resolve(ConsultationRequest request) {
        Optional<CaseDefinition> caseDefinition = caseCatalog.allCases().stream()
                .filter(candidate -> candidate.id().equals(request.getCaseId()))
                .findFirst();
        return caseDefinition.flatMap(definition -> definition
                .findCharacter(request.getAskerId())
                .flatMap(asker -> asker.findConsultation(request.getConsultationId())
                        .flatMap(consultation -> definition
                                .findCharacter(consultation.character())
                                .map(contact -> new ResolvedRequest(
                                        definition, asker, new ConsultationContact(consultation, contact))))));
    }

    private String contactAnswer(ResolvedRequest consultation, String question) {
        return characterReplyModel.reply(
                List.of(
                        new SystemMessage(characterPromptBuilder.buildContactAnswerPrompt(
                                consultation.caseDefinition(), consultation.contact(), consultation.asker())),
                        new UserMessage(characterPromptBuilder.formatContactQuestion(question))),
                List.of(),
                consultation.contact().contact().id());
    }

    private void retryOrGiveUp(ConsultationRequest request, ResolvedRequest consultation) {
        int failedAttempts = request.getFailedAttempts() + 1;
        Optional<Instant> retryAt = consultationSchedule.retryDueAt(failedAttempts);
        if (retryAt.isPresent()) {
            request.retryAt(retryAt.get());
            consultationRequestRepository.save(request);
            return;
        }
        LOG.warn("Giving up consultation request {} after {} failed attempts", request.getId(), failedAttempts);
        CharacterProfile asker = consultation.asker();
        String contactName = consultation.contact().contact().name();
        transactionTemplate.executeWithoutResult(status -> {
            consultationRequestRepository.delete(request);
            chatMessageRepository.save(ChatMessage.notice(
                    request.getTeamId(),
                    request.getCaseId(),
                    asker.id(),
                    contactName + " heeft niet gereageerd op het bericht van " + asker.name() + "."));
            eventPublisher.publishEvent(TeamEvent.chat(request.getTeamId(), request.getCaseId(), asker.id()));
        });
    }

    private record ResolvedRequest(
            CaseDefinition caseDefinition, CharacterProfile asker, ConsultationContact contact) {}
}
