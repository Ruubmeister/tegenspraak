package nl.rubium.tegenspraak.desktop.boundary;

import nl.rubium.tegenspraak.casefile.control.CaseCatalog;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Desktop content is fully visible to the player, so the entity is returned as is. */
@RestController
public class DesktopController {

    private final CaseCatalog caseCatalog;

    public DesktopController(CaseCatalog caseCatalog) {
        this.caseCatalog = caseCatalog;
    }

    @GetMapping("/api/cases/{caseId}/desktops/{desktopId}")
    public Desktop getDesktop(@PathVariable String caseId, @PathVariable String desktopId) {
        return caseCatalog.getDesktop(caseId, desktopId);
    }
}
