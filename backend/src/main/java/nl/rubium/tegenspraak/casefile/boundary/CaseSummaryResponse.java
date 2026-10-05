package nl.rubium.tegenspraak.casefile.boundary;

import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;

public record CaseSummaryResponse(String id, String title, String summary) {

    static CaseSummaryResponse from(CaseDefinition caseDefinition) {
        return new CaseSummaryResponse(
                caseDefinition.id(),
                caseDefinition.manifest().title(),
                caseDefinition.manifest().summary());
    }
}
