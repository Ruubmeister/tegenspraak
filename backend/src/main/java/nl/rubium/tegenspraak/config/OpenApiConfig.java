package nl.rubium.tegenspraak.config;

import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Shapes the OpenAPI spec that the frontend generates its API types from, see OpenApiSpecIT. */
@Configuration
public class OpenApiConfig {

    /**
     * Jackson writes every field of a response record, also when it is null, so every response property is required;
     * the ones that can be null are annotated with @Nullable. Requests keep what their constraints say is required.
     */
    @Bean
    OpenApiCustomizer requireAllResponseProperties() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (!name.endsWith("Request")) {
                requireAllProperties(schema);
            }
        });
    }

    /** Subtypes such as MailAppResponse keep their own properties in an allOf part next to the base type. */
    private static void requireAllProperties(Schema<?> schema) {
        if (schema.getProperties() != null) {
            schema.setRequired(List.copyOf(schema.getProperties().keySet()).stream()
                    .sorted()
                    .toList());
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(OpenApiConfig::requireAllProperties);
        }
    }
}
