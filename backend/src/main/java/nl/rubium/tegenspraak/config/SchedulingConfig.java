package nl.rubium.tegenspraak.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Runs scheduled game events, such as characters answering each other's questions after a delay. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
