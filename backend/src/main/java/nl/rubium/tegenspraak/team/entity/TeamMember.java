package nl.rubium.tegenspraak.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_member")
public class TeamMember {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected TeamMember() {}

    public TeamMember(UUID teamId, UUID playerId) {
        this.id = UUID.randomUUID();
        this.teamId = teamId;
        this.playerId = playerId;
        this.joinedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
