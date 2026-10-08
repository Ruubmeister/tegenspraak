package nl.rubium.tegenspraak.auth.control;

import java.util.UUID;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Ends the sessions of a player whose account was deleted or whose password was reset. Expired sessions are logged
 * out by the ConcurrentSessionFilter on their next request.
 */
@Component
public class PlayerSessions {

    private final SessionRegistry sessionRegistry;

    public PlayerSessions(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void endAll(UUID playerId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof PlayerPrincipal player
                        && player.getId().equals(playerId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }
}
