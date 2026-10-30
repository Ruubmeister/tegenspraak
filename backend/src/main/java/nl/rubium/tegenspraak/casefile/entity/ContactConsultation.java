package nl.rubium.tegenspraak.casefile.entity;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Another character this character can send a message to on behalf of the team, for example to find out a code. The
 * character's LLM decides to ask once {@code when} is met in the conversation; the contact replies after a few minutes
 * with what {@code answer} says, and the character passes it on to the team. The contact only knows the answer, not its
 * own secrets, so the question cannot talk it into more. Once answered, the {@code unlocks} open for the team. With a
 * {@code minimumMood}, the character only offers to ask once it feels at least that good about the team.
 */
public record ContactConsultation(
        @NotBlank String id,
        @NotBlank String character,
        @NotBlank String about,
        @NotBlank String when,
        @NotBlank String answer,
        Mood minimumMood,
        List<ConsultationUnlock> unlocks) {

    public ContactConsultation {
        unlocks = unlocks == null ? List.of() : List.copyOf(unlocks);
    }

    public boolean allows(Mood mood) {
        return minimumMood == null || mood.isAtLeast(minimumMood);
    }
}
