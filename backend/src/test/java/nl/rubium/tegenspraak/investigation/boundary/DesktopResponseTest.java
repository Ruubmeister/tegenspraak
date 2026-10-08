package nl.rubium.tegenspraak.investigation.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.Lock;
import nl.rubium.tegenspraak.desktop.entity.MailApp;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.investigation.boundary.DesktopResponse.MailAppResponse;
import nl.rubium.tegenspraak.investigation.control.TeamProgress;
import org.junit.jupiter.api.Test;

class DesktopResponseTest {

    private static final MailMessage LOCKED_MAIL = new MailMessage(
            "brief",
            "Inbox",
            "a@example.nl",
            "b@example.nl",
            "Geheim",
            LocalDateTime.of(2026, 1, 1, 20, 0),
            "Het testament ligt in de kluis.",
            new Lock("kandelaar", "Het wapen"));
    private static final Desktop DESKTOP = new Desktop(
            "computer",
            "Landhuis",
            OwnerType.ORGANISATION,
            "Computer in de hal.",
            new Lock("1234", "Het jaartal"),
            List.of(new MailApp("mail", "Mail", List.of(LOCKED_MAIL))));

    @Test
    void from_givenLockedDesktop_expectHintAndNoApps() {
        DesktopResponse response = DesktopResponse.from(DESKTOP, progress());

        assertThat(response.lock().hint()).isEqualTo("Het jaartal");
        assertThat(response.apps()).isEmpty();
    }

    @Test
    void from_givenOpenDesktopWithLockedMail_expectSubjectAndHintWithoutBody() {
        DesktopResponse response = DesktopResponse.from(DESKTOP, progress("desktop:computer"));

        assertThat(response.lock()).isNull();
        assertThat(response.apps())
                .singleElement()
                .isInstanceOfSatisfying(
                        MailAppResponse.class,
                        mail -> assertThat(mail.messages()).singleElement().satisfies(message -> {
                            assertThat(message.subject()).isEqualTo("Geheim");
                            assertThat(message.body()).isNull();
                            assertThat(message.lock().hint()).isEqualTo("Het wapen");
                        }));
    }

    @Test
    void from_givenUnlockedMail_expectBodyAndNoLock() {
        DesktopResponse response =
                DesktopResponse.from(DESKTOP, progress("desktop:computer", "item:computer/mail/brief"));

        assertThat(((MailAppResponse) response.apps().getFirst()).messages().getFirst())
                .satisfies(message -> {
                    assertThat(message.body()).isEqualTo("Het testament ligt in de kluis.");
                    assertThat(message.lock()).isNull();
                });
    }

    private static TeamProgress progress(String... unlockedTargets) {
        CaseDefinition caseDefinition =
                new CaseDefinition("demo", CaseFixtures.manifestWithSetting("Geheim"), List.of(), List.of(DESKTOP));
        return new TeamProgress(caseDefinition, Set.of(unlockedTargets));
    }
}
