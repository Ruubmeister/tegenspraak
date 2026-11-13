package nl.rubium.tegenspraak.casefile.entity;

import jakarta.validation.constraints.NotBlank;

/**
 * A hidden character that this character can introduce to the team, and the condition in the conversation under which
 * it does so. The condition is given to the LLM, like the reveal condition of a secret. With a {@code minimumMood}, the
 * character only offers the introduction once it feels at least that good about the team.
 */
public record ContactIntroduction(
        @NotBlank String character, @NotBlank String when, Mood minimumMood) {

    public boolean allows(Mood mood) {
        return minimumMood == null || mood.isAtLeast(minimumMood);
    }
}
