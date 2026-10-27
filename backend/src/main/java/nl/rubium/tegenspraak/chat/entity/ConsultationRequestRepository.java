package nl.rubium.tegenspraak.chat.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, UUID> {

    /** Pending and answered requests of one character, so it does not ask the same thing twice. */
    List<ConsultationRequest> findByTeamIdAndCaseIdAndAskerId(UUID teamId, String caseId, String askerId);

    List<ConsultationRequest> findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(ConsultationStatus status, Instant now);
}
