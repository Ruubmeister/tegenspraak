package nl.rubium.tegenspraak.casefile.entity;

import java.util.List;

/** Minimal valid case content for tests that do not care about the solution. */
public final class CaseFixtures {

    private CaseFixtures() {}

    public static CaseManifest manifestWithSetting(String setting) {
        List<SolutionOption> options = List.of(new SolutionOption("ja", "Ja"), new SolutionOption("nee", "Nee"));
        CaseSolution solution =
                new CaseSolution(List.of(new SolutionQuestion("dader", "De butler?", options, "ja")), "Einde.");
        return new CaseManifest("Demo", "Kort", "Intro", setting, solution);
    }
}
