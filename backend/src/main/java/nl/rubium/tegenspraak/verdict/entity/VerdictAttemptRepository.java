package nl.rubium.tegenspraak.verdict.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerdictAttemptRepository extends JpaRepository<VerdictAttempt, UUID> {

    List<VerdictAttempt> findByTeamIdAndCaseIdOrderBySubmittedAtAsc(UUID teamId, String caseId);

    @Query("select distinct attempt.caseId from VerdictAttempt attempt"
            + " where attempt.teamId = :teamId and attempt.solved = true")
    List<String> findSolvedCaseIds(@Param("teamId") UUID teamId);
}
