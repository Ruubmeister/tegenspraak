package nl.rubium.tegenspraak.liveupdate.control;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param heartbeatInterval how often open streams are pinged and checked; read by {@link TeamEventStreams} through its
 *     placeholder
 * @param streamTimeout streams end after this long and the browser reconnects, which checks the session again
 */
@Validated
@ConfigurationProperties("tegenspraak.live-updates")
public record LiveUpdateProperties(
        @NotNull Duration heartbeatInterval, @NotNull Duration streamTimeout) {}
