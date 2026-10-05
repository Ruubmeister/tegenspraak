package nl.rubium.tegenspraak.auth.control;

import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PlayerRegistrationService {

    private final PlayerAccountRepository playerAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerRegistrationService(PlayerAccountRepository playerAccountRepository, PasswordEncoder passwordEncoder) {
        this.playerAccountRepository = playerAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PlayerAccount register(String username, String rawPassword) {
        if (playerAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameTakenException(username);
        }
        PlayerAccount account = new PlayerAccount(username, passwordEncoder.encode(rawPassword));
        try {
            return playerAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            // A concurrent registration claimed the name between the check and the insert
            throw new UsernameTakenException(username);
        }
    }
}
