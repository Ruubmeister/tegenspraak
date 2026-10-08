package nl.rubium.tegenspraak.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_account")
public class PlayerAccount {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlayerAccount() {}

    public PlayerAccount(String username, String passwordHash, PlayerRole role) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public PlayerRole getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == PlayerRole.ADMIN;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
