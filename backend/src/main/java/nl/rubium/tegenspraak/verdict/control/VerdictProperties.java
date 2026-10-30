package nl.rubium.tegenspraak.verdict.control;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** @param maxAttempts a team that has not solved a case after this many attempts has failed it */
@Validated
@ConfigurationProperties("tegenspraak.verdict")
public record VerdictProperties(@Min(1) int maxAttempts) {}
