package nl.rubium.tegenspraak.casefile.entity;

import java.util.List;

/** Minimal valid case content for tests that do not care about the solution. */
public final class CaseFixtures {

    private CaseFixtures() {}

    public static CaseManifest manifestWithSetting(String setting) {
        CaseSolution solution =
                new CaseSolution(List.of(new SolutionQuestion("dader", "Wie deed het?", "De butler.")), "Einde.");
        return new CaseManifest("Demo", "Kort", CaseDifficulty.MAKKELIJK, "Intro", setting, solution);
    }
}
