package nl.rubium.tegenspraak.verdict.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** The team's typed answer to one solution question, and whether the grader judged it correct. */
@Embeddable
public record VerdictAnswer(
        @Column(name = "question_id", nullable = false) String questionId,
        @Column(nullable = false) String answer,
        @Column(nullable = false) boolean correct) {}
