package nl.rubium.tegenspraak.auth.control;

import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlayerUserDetailsService implements UserDetailsService {

    private final PlayerAccountRepository playerAccountRepository;

    public PlayerUserDetailsService(PlayerAccountRepository playerAccountRepository) {
        this.playerAccountRepository = playerAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        PlayerAccount account = playerAccountRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown player"));
        return PlayerPrincipal.from(account);
    }
}
