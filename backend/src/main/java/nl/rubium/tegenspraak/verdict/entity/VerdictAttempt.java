package nl.rubium.tegenspraak.verdict.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One attempt by a team to solve a case. Only the score and who submitted it are kept, not the chosen answers. */
@Entity
@Table(name = "verdict_attempt")
public class VerdictAttempt {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private boolean solved;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected VerdictAttempt() {}

    public VerdictAttempt(UUID teamId, String caseId, String submittedBy, int correctAnswers, int totalQuestions) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.submittedBy = submittedBy;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.solved = correctAnswers == totalQuestions;
        this.submittedAt = Instant.now();
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

    public String getSubmittedBy() {
        return submittedBy;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public boolean isSolved() {
        return solved;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
