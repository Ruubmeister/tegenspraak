package nl.rubium.tegenspraak.investigation.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WrongCodeAttemptRepository extends JpaRepository<WrongCodeAttempt, UUID> {

    List<WrongCodeAttempt> findByTeamIdAndCaseIdAndTargetAndAttemptedAtAfterOrderByAttemptedAtAsc(
            UUID teamId, String caseId, String target, Instant since);
}
