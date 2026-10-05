package nl.rubium.tegenspraak;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.auth.control.PlayerRegistrationService;
import nl.rubium.tegenspraak.auth.entity.PlayerAccount;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Full application against a Postgres Testcontainer. The LLM is mocked so tests never call a real API. */
@SpringBootTest(properties = "spring.ai.anthropic.api-key=test-key")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PlayerRegistrationService playerRegistrationService;

    @MockitoBean
    protected ChatModel chatModel;

    /** Mirrors the SPA: the XSRF-TOKEN cookie value is echoed back in the X-XSRF-TOKEN header. */
    protected static RequestPostProcessor spaCsrf() {
        return request -> {
            String token = UUID.randomUUID().toString();
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }

    /** Registers a player with a unique name, so tests do not depend on each other's data. */
    protected PlayerPrincipal registerPlayer() {
        String username = "speler-" + UUID.randomUUID().toString().substring(0, 8);
        PlayerAccount account = playerRegistrationService.register(username, "geheim-wachtwoord");
        return new PlayerPrincipal(account.getId(), account.getUsername(), account.getPasswordHash());
    }
}
