package nl.rubium.tegenspraak.chat.control;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param maxHistoryMessages how many of the latest messages a character remembers when it writes a message
 * @param toneClassifierModel the small, fast model that judges the tone of player messages
 */
@Validated
@ConfigurationProperties("tegenspraak.chat")
public record ChatProperties(
        @Positive int maxHistoryMessages, @NotBlank String toneClassifierModel) {}
