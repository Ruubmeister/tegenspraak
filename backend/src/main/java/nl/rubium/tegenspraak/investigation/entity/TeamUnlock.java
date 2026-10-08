package nl.rubium.tegenspraak.investigation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Something a team unlocked in a case: an introduced character, or a desktop, mail or file opened with its code. */
@Entity
@Table(name = "team_unlock")
public class TeamUnlock {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String target;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    protected TeamUnlock() {}

    public UUID getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getTarget() {
        return target;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }
}
