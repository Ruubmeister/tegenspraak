package nl.rubium.tegenspraak.casefile.boundary;

import java.util.List;
import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseCatalog caseCatalog;

    public CaseController(CaseCatalog caseCatalog) {
        this.caseCatalog = caseCatalog;
    }

    @GetMapping
    public List<CaseSummaryResponse> listCases() {
        return caseCatalog.allCases().stream().map(CaseSummaryResponse::from).toList();
    }

    @GetMapping("/{caseId}")
    public CaseOverviewResponse getCase(@PathVariable String caseId) {
        return CaseOverviewResponse.from(caseCatalog.getCase(caseId));
    }
}
