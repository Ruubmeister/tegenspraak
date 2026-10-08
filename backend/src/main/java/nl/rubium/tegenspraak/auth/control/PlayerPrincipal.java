package nl.rubium.tegenspraak.auth.control;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated player stored in the HTTP session. Carries the player id so controllers can scope data to the player
 * without an extra lookup. Equality on the id lets the session registry group all sessions of one player.
 */
public final class PlayerPrincipal implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final String username;
    private final PlayerRole role;
    private String passwordHash;

    public PlayerPrincipal(UUID id, String username, PlayerRole role, String passwordHash) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    public static PlayerPrincipal from(PlayerAccount account) {
        return new PlayerPrincipal(
                account.getId(), account.getUsername(), account.getRole(), account.getPasswordHash());
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public PlayerRole getRole() {
        return role;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PlayerPrincipal player && id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
