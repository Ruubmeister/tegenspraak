package nl.rubium.tegenspraak.investigation.boundary;

import jakarta.validation.Valid;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.investigation.control.CodeCooldownException;
import nl.rubium.tegenspraak.investigation.control.InvestigationService;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A case as a team sees it, and entering codes. Every response reflects what the team has unlocked so far. */
@RestController
@RequestMapping("/api/teams/{teamId}/cases/{caseId}")
public class InvestigationController {

    private final InvestigationService investigationService;
    private final CaseCatalog caseCatalog;

    public InvestigationController(InvestigationService investigationService, CaseCatalog caseCatalog) {
        this.investigationService = investigationService;
        this.caseCatalog = caseCatalog;
    }

    @GetMapping
    public CaseOverviewResponse getCase(
            @PathVariable UUID teamId, @PathVariable String caseId, @AuthenticationPrincipal PlayerPrincipal player) {
        return CaseOverviewResponse.from(investigationService.getProgress(teamId, player.getId(), caseId));
    }

    @GetMapping("/desktops/{desktopId}")
    public DesktopResponse getDesktop(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @PathVariable String desktopId,
            @AuthenticationPrincipal PlayerPrincipal player) {
        TeamProgress progress = investigationService.getProgress(teamId, player.getId(), caseId);
        return DesktopResponse.from(caseCatalog.getDesktop(caseId, desktopId), progress);
    }

    @PostMapping("/desktops/{desktopId}/unlock")
    public DesktopResponse unlockDesktop(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @PathVariable String desktopId,
            @Valid @RequestBody UnlockRequest request,
            @AuthenticationPrincipal PlayerPrincipal player) {
        TeamProgress progress =
                investigationService.unlockDesktop(teamId, player.getId(), caseId, desktopId, request.code());
        return DesktopResponse.from(caseCatalog.getDesktop(caseId, desktopId), progress);
    }

    @PostMapping("/desktops/{desktopId}/apps/{appId}/items/{itemId}/unlock")
    public DesktopResponse unlockItem(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @PathVariable String desktopId,
            @PathVariable String appId,
            @PathVariable String itemId,
            @Valid @RequestBody UnlockRequest request,
            @AuthenticationPrincipal PlayerPrincipal player) {
        TeamProgress progress = investigationService.unlockItem(
                teamId, player.getId(), caseId, desktopId, appId, itemId, request.code());
        return DesktopResponse.from(caseCatalog.getDesktop(caseId, desktopId), progress);
    }

    @ExceptionHandler(CodeCooldownException.class)
    ResponseEntity<ProblemDetail> handleCooldown(CodeCooldownException exception) {
        // Rounded up, so a client that waits this long is never refused again
        long retryAfterSeconds = Math.max(1, (exception.getRetryAfter().toMillis() + 999) / 1000);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong codes, try again later");
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
                .body(problem);
    }
}
