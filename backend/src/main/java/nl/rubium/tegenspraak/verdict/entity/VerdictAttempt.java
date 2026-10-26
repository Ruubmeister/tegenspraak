package nl.rubium.tegenspraak.verdict.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * One attempt by a team to solve a case, with the typed answers. Attempts are numbered per team and case; the database
 * rejects a second attempt with the same number, so concurrent submits cannot exceed the limit.
 */
@Entity
@Table(name = "verdict_attempt")
public class VerdictAttempt {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

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

    /** Loaded eagerly in batches: attempts are always shown with their answers. */
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @CollectionTable(name = "verdict_attempt_answer", joinColumns = @JoinColumn(name = "attempt_id"))
    private List<VerdictAnswer> answers = new ArrayList<>();

    protected VerdictAttempt() {}

    public VerdictAttempt(
            UUID teamId, String caseId, int attemptNumber, String submittedBy, List<VerdictAnswer> answers) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.attemptNumber = attemptNumber;
        this.submittedBy = submittedBy;
        this.answers = new ArrayList<>(answers);
        this.correctAnswers =
                (int) answers.stream().filter(VerdictAnswer::correct).count();
        this.totalQuestions = answers.size();
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

    public int getAttemptNumber() {
        return attemptNumber;
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

    /** Empty for attempts from before answers were typed. */
    public List<VerdictAnswer> getAnswers() {
        return List.copyOf(answers);
    }
}
