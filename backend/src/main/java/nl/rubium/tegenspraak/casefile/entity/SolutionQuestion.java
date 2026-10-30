package nl.rubium.tegenspraak.casefile.entity;

import jakarta.validation.constraints.NotBlank;

/**
 * An open question the team answers in its own words. {@code answer} describes what a correct answer must contain; the
 * grader uses it as the rubric and the team never sees it.
 */
public record SolutionQuestion(
        @NotBlank String id,
        @NotBlank String question,
        @NotBlank String answer) {}
