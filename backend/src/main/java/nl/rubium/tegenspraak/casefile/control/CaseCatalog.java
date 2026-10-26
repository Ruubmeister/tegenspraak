package nl.rubium.tegenspraak.casefile.control;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.Photo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class CaseCatalog {

    private final Map<String, CaseDefinition> casesById = new LinkedHashMap<>();
    private final CaseFileLoader caseFileLoader;
    private final String casesLocation;

    public CaseCatalog(CaseFileLoader caseFileLoader, @Value("${tegenspraak.cases.location}") String casesLocation) {
        this.caseFileLoader = caseFileLoader;
        this.casesLocation = casesLocation;
        caseFileLoader
                .loadAll(casesLocation)
                .forEach(caseDefinition -> casesById.put(caseDefinition.id(), caseDefinition));
    }

    public List<CaseDefinition> allCases() {
        return List.copyOf(casesById.values());
    }

    public CaseDefinition getCase(String caseId) {
        CaseDefinition caseDefinition = casesById.get(caseId);
        if (caseDefinition == null) {
            throw new CaseContentNotFoundException("Unknown case: " + caseId);
        }
        return caseDefinition;
    }

    public CharacterProfile getCharacter(String caseId, String characterId) {
        return getCase(caseId)
                .findCharacter(characterId)
                .orElseThrow(() -> new CaseContentNotFoundException("Unknown character: " + characterId));
    }

    public Desktop getDesktop(String caseId, String desktopId) {
        return getCase(caseId)
                .findDesktop(desktopId)
                .orElseThrow(() -> new CaseContentNotFoundException("Unknown desktop: " + desktopId));
    }

    public Resource getPhotoImage(String caseId, String desktopId, Photo photo) {
        return caseFileLoader.photoImage(casesLocation + "/" + getCase(caseId).id(), desktopId, photo);
    }
}
