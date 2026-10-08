package nl.rubium.tegenspraak.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A group of players that investigates cases together. Players join with the join code. */
@Entity
@Table(name = "team")
public class Team {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "join_code", nullable = false)
    private String joinCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Team() {}

    public Team(String name, String joinCode) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.joinCode = joinCode;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
