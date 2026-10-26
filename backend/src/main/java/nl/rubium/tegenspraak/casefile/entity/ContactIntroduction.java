package nl.rubium.tegenspraak.casefile.entity;

/**
 * A hidden character that this character can introduce to the team, and the condition in the conversation under which
 * it does so. The condition is given to the LLM, like the reveal condition of a secret. With a {@code minimumMood}, the
 * character only offers the introduction once it feels at least that good about the team.
 */
public record ContactIntroduction(String character, String when, Mood minimumMood) {

    public ContactIntroduction {
        if (character == null || character.isBlank() || when == null || when.isBlank()) {
            throw new IllegalArgumentException("A contact introduction needs a character and a condition");
        }
    }

    public boolean allows(Mood mood) {
        return minimumMood == null || mood.isAtLeast(minimumMood);
    }
}
