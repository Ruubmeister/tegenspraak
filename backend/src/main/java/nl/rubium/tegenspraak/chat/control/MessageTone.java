package nl.rubium.tegenspraak.chat.control;

/**
 * The tone of a team's message to a character, and how much it changes the character's mood score. Helping or
 * convincing the character makes it friendlier, accusing or attacking it makes it like the team less.
 */
public enum MessageTone {
    HELPFUL(15),
    CONVINCING(10),
    NEUTRAL(0),
    ACCUSING(-10),
    AGGRESSIVE(-25);

    private final int scoreChange;

    MessageTone(int scoreChange) {
        this.scoreChange = scoreChange;
    }

    public int scoreChange() {
        return scoreChange;
    }
}
