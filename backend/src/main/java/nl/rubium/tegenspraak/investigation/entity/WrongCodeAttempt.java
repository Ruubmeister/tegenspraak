package nl.rubium.tegenspraak.investigation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A wrong code entered by a team, kept to slow down guessing. */
@Entity
@Table(name = "wrong_code_attempt")
public class WrongCodeAttempt {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String target;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected WrongCodeAttempt() {}

    public WrongCodeAttempt(UUID teamId, String caseId, String target, Instant attemptedAt) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.target = target;
        this.attemptedAt = attemptedAt;
    }

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

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
