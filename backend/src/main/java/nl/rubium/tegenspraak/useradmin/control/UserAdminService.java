package nl.rubium.tegenspraak.useradmin.control;

import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerAccountService;
import nl.rubium.tegenspraak.auth.control.PlayerSessions;
import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import nl.rubium.tegenspraak.team.control.TeamService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management for the admin. Only player accounts can be changed here; the single admin account is protected so
 * the system can never lose its admin.
 */
@Service
public class UserAdminService {

    private final PlayerAccountRepository playerAccountRepository;
    private final PlayerAccountService playerAccountService;
    private final PlayerSessions playerSessions;
    private final PasswordEncoder passwordEncoder;
    private final TeamService teamService;

    public UserAdminService(
            PlayerAccountRepository playerAccountRepository,
            PlayerAccountService playerAccountService,
            PlayerSessions playerSessions,
            PasswordEncoder passwordEncoder,
            TeamService teamService) {
        this.playerAccountRepository = playerAccountRepository;
        this.playerAccountService = playerAccountService;
        this.playerSessions = playerSessions;
        this.passwordEncoder = passwordEncoder;
        this.teamService = teamService;
    }

    public List<PlayerAccount> listUsers() {
        return playerAccountRepository.findAllByOrderByUsernameAsc();
    }

    public PlayerAccount createPlayer(String username, String rawPassword) {
        return playerAccountService.createPlayer(username, rawPassword);
    }

    /** The player has to log in again with the new password. */
    @Transactional
    public void resetPassword(UUID playerId, String newPassword) {
        PlayerAccount account = findManageablePlayer(playerId);
        account.changePasswordHash(passwordEncoder.encode(newPassword));
        playerSessions.endAll(playerId);
    }

    /**
     * The database removes the player's team memberships (ON DELETE CASCADE). Their messages stay in the team chats;
     * teams left without members are deleted.
     */
    @Transactional
    public void deletePlayer(UUID playerId) {
        PlayerAccount account = findManageablePlayer(playerId);
        playerAccountRepository.delete(account);
        playerAccountRepository.flush();
        teamService.deleteTeamsWithoutMembers();
        playerSessions.endAll(playerId);
    }

    private PlayerAccount findManageablePlayer(UUID playerId) {
        PlayerAccount account =
                playerAccountRepository.findById(playerId).orElseThrow(() -> new UserNotFoundException(playerId));
        if (account.isAdmin()) {
            throw new AdminAccountProtectedException();
        }
        return account;
    }
}
