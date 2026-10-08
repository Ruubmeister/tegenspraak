package nl.rubium.tegenspraak.auth.control;

import java.util.UUID;
import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates accounts and changes passwords. Accounts are only created by the admin, there is no self registration. */
@Service
public class PlayerAccountService {

    private final PlayerAccountRepository playerAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerAccountService(PlayerAccountRepository playerAccountRepository, PasswordEncoder passwordEncoder) {
        this.playerAccountRepository = playerAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PlayerAccount createPlayer(String username, String rawPassword) {
        return create(username, rawPassword, PlayerRole.PLAYER);
    }

    public PlayerAccount createAdmin(String username, String rawPassword) {
        return create(username, rawPassword, PlayerRole.ADMIN);
    }

    /** Checks the current password first, so a hijacked session alone cannot take over the account. */
    @Transactional
    public void changeOwnPassword(UUID playerId, String currentPassword, String newPassword) {
        PlayerAccount account = playerAccountRepository
                .findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Logged in player no longer exists: " + playerId));
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }
        account.changePasswordHash(passwordEncoder.encode(newPassword));
    }

    private PlayerAccount create(String username, String rawPassword, PlayerRole role) {
        if (playerAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameTakenException(username);
        }
        PlayerAccount account = new PlayerAccount(username, passwordEncoder.encode(rawPassword), role);
        try {
            return playerAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request claimed the name between the check and the insert
            throw new UsernameTakenException(username);
        }
    }
}
