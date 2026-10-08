package nl.rubium.tegenspraak.verdict.boundary;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import nl.rubium.tegenspraak.auth.control.PlayerPrincipal;
import nl.rubium.tegenspraak.verdict.control.VerdictService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerdictController {

    private final VerdictService verdictService;

    public VerdictController(VerdictService verdictService) {
        this.verdictService = verdictService;
    }

    @GetMapping("/api/teams/{teamId}/cases/{caseId}/verdict")
    public VerdictResponse getVerdict(
            @PathVariable UUID teamId, @PathVariable String caseId, @AuthenticationPrincipal PlayerPrincipal player) {
        return VerdictResponse.from(verdictService.getProgress(teamId, player.getId(), caseId));
    }

    @PostMapping("/api/teams/{teamId}/cases/{caseId}/verdict")
    public VerdictResponse submitVerdict(
            @PathVariable UUID teamId,
            @PathVariable String caseId,
            @Valid @RequestBody SubmitVerdictRequest request,
            @AuthenticationPrincipal PlayerPrincipal player) {
        return VerdictResponse.from(
                verdictService.submit(teamId, player.getId(), player.getUsername(), caseId, request.answers()));
    }

    @GetMapping("/api/teams/{teamId}/solved-cases")
    public List<String> getSolvedCaseIds(@PathVariable UUID teamId, @AuthenticationPrincipal PlayerPrincipal player) {
        return verdictService.getSolvedCaseIds(teamId, player.getId());
    }
}
