package nl.rubium.tegenspraak.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Up to which message a player has read a team's conversation with a character. Each team member has their own, so a
 * reply a teammate already read is still new for the others. Written through
 * {@link ConversationReadRepository#markReadUntil}.
 */
@Entity
@Table(name = "conversation_read")
public class ConversationRead {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Column(name = "read_until", nullable = false)
    private Instant readUntil;

    protected ConversationRead() {}

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
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

    public Instant getReadUntil() {
        return readUntil;
    }
}
