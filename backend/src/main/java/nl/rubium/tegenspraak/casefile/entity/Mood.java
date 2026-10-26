package nl.rubium.tegenspraak.casefile.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How a character feels about the team, from worst to best. The mood follows from a score between {@link #MIN_SCORE}
 * and {@link #MAX_SCORE} that shifts with the tone of the team's messages. A character starts at the starting score of
 * its starting mood.
 */
public enum Mood {
    @JsonProperty("hostile")
    HOSTILE(-70),
    @JsonProperty("suspicious")
    SUSPICIOUS(-35),
    @JsonProperty("neutral")
    NEUTRAL(0),
    @JsonProperty("friendly")
    FRIENDLY(35),
    @JsonProperty("trusting")
    TRUSTING(70);

    public static final int MIN_SCORE = -100;
    public static final int MAX_SCORE = 100;

    private final int startingScore;

    Mood(int startingScore) {
        this.startingScore = startingScore;
    }

    public int startingScore() {
        return startingScore;
    }

    public static Mood fromScore(int score) {
        if (score <= -60) {
            return HOSTILE;
        }
        if (score <= -20) {
            return SUSPICIOUS;
        }
        if (score < 20) {
            return NEUTRAL;
        }
        if (score < 60) {
            return FRIENDLY;
        }
        return TRUSTING;
    }

    public boolean isAtLeast(Mood other) {
        return compareTo(other) >= 0;
    }
}
