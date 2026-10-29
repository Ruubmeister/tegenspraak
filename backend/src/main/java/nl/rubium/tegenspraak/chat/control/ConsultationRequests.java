package nl.rubium.tegenspraak.chat.control;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ContactConsultation;
import nl.rubium.tegenspraak.casefile.entity.Mood;
import nl.rubium.tegenspraak.chat.entity.ChatMessage;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequest;
import nl.rubium.tegenspraak.chat.entity.ConsultationRequestRepository;
import nl.rubium.tegenspraak.chat.entity.ConsultationStatus;
import org.springframework.stereotype.Component;

/**
 * The questions a character asks other characters for the team: which it may still ask, which answers it is waiting
 * for, and storing new questions so ConsultationService answers them once they are due.
 */
@Component
class ConsultationRequests {

    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationSchedule consultationSchedule;

    ConsultationRequests(
            ConsultationRequestRepository consultationRequestRepository, ConsultationSchedule consultationSchedule) {
        this.consultationRequestRepository = consultationRequestRepository;
        this.consultationSchedule = consultationSchedule;
    }

    /**
     * Consultations the character may still start: not asked before by this team, and allowed by its mood. Pending
     * ones are listed too, so the character knows it is still waiting for an answer.
     */
    ConsultationPrompt forCharacter(
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

    /** Stores a request per question, answered once it is due, and returns the notices for them. */
    List<ChatMessage> schedule(
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
            notices.add(conversation.notice(
                    character.name() + " heeft " + question.contact().contact().name() + " een bericht gestuurd."));
        }
        return notices;
    }

    private static Optional<ConsultationContact> withContact(
            CaseDefinition caseDefinition, ContactConsultation consultation) {
        return caseDefinition
                .findCharacter(consultation.character())
                .map(contact -> new ConsultationContact(consultation, contact));
    }
}
