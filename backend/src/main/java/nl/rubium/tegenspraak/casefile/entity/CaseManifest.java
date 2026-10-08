package nl.rubium.tegenspraak.casefile.entity;

import java.util.Objects;

public record CaseManifest(String title, String summary, String introduction, String setting, CaseSolution solution) {

    public CaseManifest {
        Objects.requireNonNull(solution, "A case needs a solution");
    }
}
