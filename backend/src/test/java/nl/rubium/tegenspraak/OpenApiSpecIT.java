package nl.rubium.tegenspraak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Keeps the OpenAPI spec that the frontend generates its API types from in sync with the backend. When the API
 * changed, this test writes the new spec and fails; commit the spec and run "npm run generate:api" in the frontend.
 */
class OpenApiSpecIT extends IntegrationTest {

    private static final Path SPEC_FILE = Path.of("../frontend/openapi.json");

    @Test
    void apiDocs_givenCurrentControllers_expectCommittedSpecUpToDate() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs").with(user(createPlayer())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8)
                + "\n";
        String committedSpec = Files.exists(SPEC_FILE) ? Files.readString(SPEC_FILE) : "";
        if (!spec.equals(committedSpec)) {
            Files.writeString(SPEC_FILE, spec);
        }

        assertThat(committedSpec)
                .as(
                        "The API changed: %s was updated, commit it and run \"npm run generate:api\" in the frontend",
                        SPEC_FILE)
                .isEqualTo(spec);
    }
}
