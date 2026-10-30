package nl.rubium.tegenspraak.casefile.control;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** @param location resource location of the case folders, see {@link CaseFileLoader} */
@Validated
@ConfigurationProperties("tegenspraak.cases")
public record CaseProperties(@NotBlank String location) {}
