package nl.rubium.tegenspraak.auth.boundary;

import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;

public record PlayerResponse(String username, PlayerRole role) {

    static PlayerResponse from(PlayerPrincipal player) {
        return new PlayerResponse(player.getUsername(), player.getRole());
    }
}
