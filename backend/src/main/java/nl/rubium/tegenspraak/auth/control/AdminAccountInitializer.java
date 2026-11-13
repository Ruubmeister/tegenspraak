package nl.rubium.tegenspraak.auth.control;

import java.security.SecureRandom;
import java.util.Base64;
import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the admin account on the first startup. The password comes from tegenspraak.admin.initial-password; when
 * that is empty a random password is generated and logged once, so a fresh install is never left with a known default.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int GENERATED_PASSWORD_BYTES = 18;

    private final PlayerAccountRepository playerAccountRepository;
    private final PlayerAccountService playerAccountService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String adminUsername;
    private final String initialPassword;

    public AdminAccountInitializer(
            PlayerAccountRepository playerAccountRepository,
            PlayerAccountService playerAccountService,
            AdminProperties adminProperties) {
        this.playerAccountRepository = playerAccountRepository;
        this.playerAccountService = playerAccountService;
        this.adminUsername = adminProperties.username();
        this.initialPassword = adminProperties.initialPassword();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (playerAccountRepository.existsByRole(PlayerRole.ADMIN)) {
            return;
        }
        if (initialPassword.isBlank()) {
            String generatedPassword = generatePassword();
            createAdmin(generatedPassword);
            log.warn(
                    "Created admin account '{}' with generated password: {} (set TEGENSPRAAK_ADMIN_PASSWORD to choose"
                            + " it yourself, and change it after logging in)",
                    adminUsername,
                    generatedPassword);
            return;
        }
        validateInitialPassword();
        createAdmin(initialPassword);
        log.info("Created admin account '{}' with the configured initial password", adminUsername);
    }

    private void createAdmin(String password) {
        try {
            playerAccountService.createAdmin(adminUsername, password);
        } catch (UsernameTakenException e) {
            throw new IllegalStateException(
                    "Cannot create admin account: username '" + adminUsername + "' belongs to a player", e);
        }
    }

    private void validateInitialPassword() {
        int length = initialPassword.length();
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException("tegenspraak.admin.initial-password must be 8 to 72 characters long");
        }
    }

    private String generatePassword() {
        byte[] bytes = new byte[GENERATED_PASSWORD_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
