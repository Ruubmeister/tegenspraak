package nl.rubium.tegenspraak.casefile.entity;

/**
 * A hidden character that this character can introduce to the team, and the condition in the conversation under which
 * it does so. The condition is given to the LLM, like the reveal condition of a secret.
 */
public record ContactIntroduction(String character, String when) {

    public ContactIntroduction {
        if (character == null || character.isBlank() || when == null || when.isBlank()) {
            throw new IllegalArgumentException("A contact introduction needs a character and a condition");
        }
    }
}
