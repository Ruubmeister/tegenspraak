package nl.rubium.tegenspraak.casefile.boundary;

import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseDifficulty;

public record CaseSummaryResponse(String id, String title, String summary, CaseDifficulty difficulty) {

    static CaseSummaryResponse from(CaseDefinition caseDefinition) {
        return new CaseSummaryResponse(
                caseDefinition.id(),
                caseDefinition.manifest().title(),
                caseDefinition.manifest().summary(),
                caseDefinition.manifest().difficulty());
    }
}
