package nl.rubium.tegenspraak.useradmin.boundary;

import java.time.Instant;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;

public record UserResponse(UUID id, String username, PlayerRole role, Instant createdAt) {

    static UserResponse from(PlayerAccount account) {
        return new UserResponse(account.getId(), account.getUsername(), account.getRole(), account.getCreatedAt());
    }
}
