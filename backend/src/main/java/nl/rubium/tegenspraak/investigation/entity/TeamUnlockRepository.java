package nl.rubium.tegenspraak.investigation.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamUnlockRepository extends JpaRepository<TeamUnlock, UUID> {

    @Query("select unlock.target from TeamUnlock unlock where unlock.teamId = :teamId and unlock.caseId = :caseId")
    List<String> findTargets(@Param("teamId") UUID teamId, @Param("caseId") String caseId);

    /**
     * Teammates may unlock the same target at the same moment; the unique constraint keeps one row.
     *
     * @return 1 when the target was newly unlocked, 0 when the team already had it
     */
    @Modifying
    @Query(
            value = "insert into team_unlock (id, team_id, case_id, target, unlocked_at)"
                    + " values (:id, :teamId, :caseId, :target, :unlockedAt) on conflict do nothing",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("teamId") UUID teamId,
            @Param("caseId") String caseId,
            @Param("target") String target,
            @Param("unlockedAt") Instant unlockedAt);
}
