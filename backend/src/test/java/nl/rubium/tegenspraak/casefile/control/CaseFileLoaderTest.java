package nl.rubium.tegenspraak.casefile.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseSolution;
import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.SolutionOption;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.FilesApp;
import nl.rubium.tegenspraak.desktop.entity.MailApp;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CaseFileLoaderTest {

    private final CaseFileLoader loader = new CaseFileLoader(new PathMatchingResourcePatternResolver());

    @Test
    void loadAll_givenValidCaseFolder_expectIdsTakenFromFolderAndFileNames() {
        List<CaseDefinition> cases = loader.loadAll("classpath:test-cases/valid");

        assertThat(cases).singleElement().satisfies(caseDefinition -> {
            assertThat(caseDefinition.id()).isEqualTo("demo-case");
            assertThat(caseDefinition.manifest().setting()).isEqualTo("De butler deed het.");
            assertThat(caseDefinition.characters())
                    .extracting(CharacterProfile::id)
                    .containsExactly("de-butler");
            assertThat(caseDefinition.desktops()).extracting(Desktop::id).containsExactly("landhuis");
        });
    }

    @Test
    void loadAll_givenCharacterWithSecrets_expectFullPersonaLoaded() {
        CharacterProfile butler = loader.loadAll("classpath:test-cases/valid")
                .getFirst()
                .characters()
                .getFirst();

        assertThat(butler.knownFacts()).containsExactly("Het diner begon om acht uur.");
        assertThat(butler.secrets()).singleElement().satisfies(secret -> {
            assertThat(secret.fact()).isEqualTo("Hij was in de bibliotheek.");
            assertThat(secret.revealWhen()).isEqualTo("De speler noemt de bibliotheek.");
        });
    }

    @Test
    void loadAll_givenDesktopWithMailAndFiles_expectTypedApps() {
        Desktop desktop = loader.loadAll("classpath:test-cases/valid")
                .getFirst()
                .desktops()
                .getFirst();

        assertThat(desktop.ownerType()).isEqualTo(OwnerType.ORGANISATION);
        assertThat(desktop.apps()).hasSize(2);
        assertThat(desktop.apps().get(0))
                .isInstanceOfSatisfying(
                        MailApp.class,
                        mail -> assertThat(mail.messages().getFirst().sentAt())
                                .isEqualTo(LocalDateTime.of(2026, 1, 1, 20, 0)));
        assertThat(desktop.apps().get(1))
                .isInstanceOfSatisfying(
                        FilesApp.class,
                        files -> assertThat(files.files().getFirst().name()).isEqualTo("menu.txt"));
    }

    @Test
    void loadAll_givenCaseWithSolution_expectQuestionsAndEpilogueLoaded() {
        CaseSolution solution = loader.loadAll("classpath:test-cases/valid")
                .getFirst()
                .manifest()
                .solution();

        assertThat(solution.epilogue()).isEqualTo("De butler bekende alles.");
        assertThat(solution.questions()).singleElement().satisfies(question -> {
            assertThat(question.id()).isEqualTo("dader");
            assertThat(question.options()).extracting(SolutionOption::id).containsExactly("de-butler", "de-tuinman");
            assertThat(question.answer()).isEqualTo("de-butler");
        });
    }

    @Test
    void loadAll_givenSolutionAnswerThatIsNotAnOption_expectStartupFailure() {
        assertThatThrownBy(() -> loader.loadAll("classpath:test-cases/invalid-solution"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("case.yaml")
                .rootCause()
                .hasMessageContaining("dader");
    }

    @Test
    void loadAll_givenUnknownPropertyInCharacterFile_expectStartupFailure() {
        assertThatThrownBy(() -> loader.loadAll("classpath:test-cases/unknown-property"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("de-butler.yaml");
    }

    @Test
    void loadAll_givenFileNameThatIsNotKebabCase_expectStartupFailure() {
        assertThatThrownBy(() -> loader.loadAll("classpath:test-cases/invalid-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("De_Butler");
    }

    @Test
    void loadAll_givenBundledCases_expectSampleCaseLoads() {
        List<CaseDefinition> cases = loader.loadAll("classpath:cases");

        assertThat(cases).extracting(CaseDefinition::id).contains("het-lek-bij-van-dijk");
    }
}
