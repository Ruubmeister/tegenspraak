package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import nl.rubium.tegenspraak.casefile.entity.Mood;

/**
 * How a character feels about a team within a case. The score shifts with every message the team sends the character
 * and stays between {@link Mood#MIN_SCORE} and {@link Mood#MAX_SCORE}. The players never see it, they can only tell from
 * how the character answers.
 */
@Entity
@Table(name = "character_mood")
public class CharacterMood {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Column(nullable = false)
    private int score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CharacterMood() {}

    private CharacterMood(UUID teamId, String caseId, String characterId, int score) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.caseId = caseId;
        this.characterId = characterId;
        this.score = score;
        this.updatedAt = Instant.now();
    }

    /** The mood of a character the team has not talked to yet. */
    public static CharacterMood start(UUID teamId, String caseId, String characterId, Mood startingMood) {
        return new CharacterMood(teamId, caseId, characterId, startingMood.startingScore());
    }

    public void changeScore(int change) {
        score = Math.clamp((long) score + change, Mood.MIN_SCORE, Mood.MAX_SCORE);
        updatedAt = Instant.now();
    }

    public Mood getMood() {
        return Mood.fromScore(score);
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

    public String getCharacterId() {
        return characterId;
    }

    public int getScore() {
        return score;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
