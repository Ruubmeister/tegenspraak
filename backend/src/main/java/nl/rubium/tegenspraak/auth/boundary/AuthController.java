package nl.rubium.tegenspraak.auth.boundary;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import nl.rubium.tegenspraak.auth.control.PlayerAccountService;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session based login for the SPA. Logout is handled by the Spring Security logout filter. Accounts are created by the
 * admin, so there is no registration endpoint.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final PlayerAccountService playerAccountService;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            PlayerAccountService playerAccountService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.playerAccountService = playerAccountService;
    }

    @PostMapping("/login")
    public PlayerResponse login(
            @Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        return logIntoSession(loginRequest.username(), loginRequest.password(), request, response);
    }

    @GetMapping("/me")
    public PlayerResponse currentPlayer(@AuthenticationPrincipal PlayerPrincipal player) {
        return PlayerResponse.from(player);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal PlayerPrincipal player) {
        playerAccountService.changeOwnPassword(player.getId(), request.currentPassword(), request.newPassword());
    }

    /**
     * Authenticates the credentials and stores the result in the session. A failed authentication throws an
     * AuthenticationException, which the security filter chain turns into a 401.
     */
    private PlayerResponse logIntoSession(
            String username, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        renderRotatedCsrfToken(request);

        return PlayerResponse.from((PlayerPrincipal) authentication.getPrincipal());
    }

    /** Login replaces the CSRF token; reading it writes the new XSRF-TOKEN cookie into this response. */
    private static void renderRotatedCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
    }
}
