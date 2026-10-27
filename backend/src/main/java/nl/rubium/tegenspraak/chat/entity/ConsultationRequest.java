package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A question a character sent another character on behalf of a team, see
 * {@link nl.rubium.tegenspraak.casefile.entity.ContactConsultation}. The question is written by the asking character's
 * LLM. It is answered once the due time has passed; a failed answer is retried later.
 */
@Entity
@Table(name = "consultation_request")
public class ConsultationRequest {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "asker_id", nullable = false)
    private String askerId;

    @Column(name = "consultation_id", nullable = false)
    private String consultationId;

    @Column(nullable = false)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    protected ConsultationRequest() {}

    public ConsultationRequest(
            UUID teamId,
            String caseId,
            String askerId,
            String consultationId,
            String question,
            Instant requestedAt,
            Instant dueAt) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.askerId = askerId;
        this.consultationId = consultationId;
        this.question = question;
        this.status = ConsultationStatus.PENDING;
        this.failedAttempts = 0;
        this.requestedAt = requestedAt;
        this.dueAt = dueAt;
    }

    public void markAnswered() {
        status = ConsultationStatus.ANSWERED;
    }

    public void retryAt(Instant nextDueAt) {
        failedAttempts++;
        dueAt = nextDueAt;
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

    public String getAskerId() {
        return askerId;
    }

    public String getConsultationId() {
        return consultationId;
    }

    public String getQuestion() {
        return question;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }
}
