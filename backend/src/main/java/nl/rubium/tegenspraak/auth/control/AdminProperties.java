package nl.rubium.tegenspraak.auth.control;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param username name of the admin account created on the first startup
 * @param initialPassword password of that account; empty means a random one is generated, see
 *     {@link AdminAccountInitializer}
 */
@Validated
@ConfigurationProperties("tegenspraak.admin")
public record AdminProperties(
        @NotBlank String username, @DefaultValue("") String initialPassword) {}
