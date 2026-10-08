package nl.rubium.tegenspraak.auth.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import nl.rubium.tegenspraak.auth.entity.PlayerAccountRepository;
import nl.rubium.tegenspraak.auth.entity.PlayerRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminAccountInitializerTest {

    private final PlayerAccountRepository playerAccountRepository = mock(PlayerAccountRepository.class);
    private final PlayerAccountService playerAccountService = mock(PlayerAccountService.class);

    @Test
    void run_givenAdminExists_expectNoAccountCreated() {
        when(playerAccountRepository.existsByRole(PlayerRole.ADMIN)).thenReturn(true);

        initializer("configured-password").run(null);

        verify(playerAccountService, never()).createAdmin(anyString(), anyString());
    }

    @Test
    void run_givenConfiguredPassword_expectAdminCreatedWithThatPassword() {
        initializer("configured-password").run(null);

        verify(playerAccountService).createAdmin("admin", "configured-password");
    }

    @Test
    void run_givenNoConfiguredPassword_expectAdminCreatedWithRandomPassword() {
        AdminAccountInitializer initializer = initializer("");
        ArgumentCaptor<String> passwords = ArgumentCaptor.forClass(String.class);

        initializer.run(null);
        initializer.run(null);

        verify(playerAccountService, times(2)).createAdmin(eq("admin"), passwords.capture());
        assertThat(passwords.getAllValues().getFirst())
                .hasSize(24)
                .isNotEqualTo(passwords.getAllValues().getLast());
    }

    @Test
    void run_givenTooShortConfiguredPassword_expectIllegalState() {
        assertThatThrownBy(() -> initializer("kort").run(null)).isInstanceOf(IllegalStateException.class);

        verify(playerAccountService, never()).createAdmin(anyString(), anyString());
    }

    @Test
    void run_givenUsernameTakenByPlayer_expectIllegalState() {
        when(playerAccountService.createAdmin(eq("admin"), any())).thenThrow(new UsernameTakenException("admin"));

        assertThatThrownBy(() -> initializer("configured-password").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("belongs to a player");
    }

    private AdminAccountInitializer initializer(String initialPassword) {
        return new AdminAccountInitializer(playerAccountRepository, playerAccountService, "admin", initialPassword);
    }
}
