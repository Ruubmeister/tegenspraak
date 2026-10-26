package nl.rubium.tegenspraak.investigation.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import nl.rubium.tegenspraak.casefile.entity.CaseDefinition;
import nl.rubium.tegenspraak.casefile.entity.CaseFixtures;
import nl.rubium.tegenspraak.desktop.entity.BrowserApp;
import nl.rubium.tegenspraak.desktop.entity.BrowserPage;
import nl.rubium.tegenspraak.desktop.entity.CalendarApp;
import nl.rubium.tegenspraak.desktop.entity.CalendarEvent;
import nl.rubium.tegenspraak.desktop.entity.Desktop;
import nl.rubium.tegenspraak.desktop.entity.Lock;
import nl.rubium.tegenspraak.desktop.entity.MailApp;
import nl.rubium.tegenspraak.desktop.entity.MailMessage;
import nl.rubium.tegenspraak.desktop.entity.OwnerType;
import nl.rubium.tegenspraak.desktop.entity.Photo;
import nl.rubium.tegenspraak.desktop.entity.PhotosApp;
import nl.rubium.tegenspraak.investigation.boundary.DesktopResponse.BrowserAppResponse;
import nl.rubium.tegenspraak.investigation.boundary.DesktopResponse.CalendarAppResponse;
import nl.rubium.tegenspraak.investigation.boundary.DesktopResponse.MailAppResponse;
import nl.rubium.tegenspraak.investigation.boundary.DesktopResponse.PhotosAppResponse;
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
    private static final CalendarEvent LOCKED_EVENT = new CalendarEvent(
            "notaris",
            "Afspraak",
            LocalDateTime.of(2026, 1, 2, 9, 0),
            LocalDateTime.of(2026, 1, 2, 10, 0),
            "Notariskantoor",
            List.of("De Butler"),
            "Testament wijzigen.",
            new Lock("kandelaar", "Het wapen"));
    private static final Photo LOCKED_PHOTO = new Photo(
            "portret",
            "Familie",
            "Portret",
            null,
            "portret.jpg",
            "Een man met een kandelaar.",
            new Lock("kandelaar", "Het wapen"));
    private static final BrowserPage LOCKED_PAGE = new BrowserPage(
            "zoekopdracht",
            "https://www.zoeken.nl/?q=gif",
            "gif kopen - Zoeken",
            LocalDateTime.of(2026, 1, 1, 22, 0),
            "Waar koop ik gif?",
            new Lock("kandelaar", "Het wapen"));
    private static final Desktop DESKTOP = new Desktop(
            "computer",
            "Landhuis",
            OwnerType.ORGANISATION,
            "Computer in de hal.",
            new Lock("1234", "Het jaartal"),
            List.of(
                    new MailApp("mail", "Mail", List.of(LOCKED_MAIL)),
                    new CalendarApp("agenda", "Agenda", List.of(LOCKED_EVENT)),
                    new PhotosApp("fotos", "Foto's", List.of(LOCKED_PHOTO)),
                    new BrowserApp("browser", "Browser", List.of(LOCKED_PAGE))));

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
        assertThat(response.apps().getFirst())
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

    @Test
    void from_givenLockedEvent_expectTitleAndTimeWithoutDetails() {
        DesktopResponse response = DesktopResponse.from(DESKTOP, progress("desktop:computer"));

        assertThat(((CalendarAppResponse) response.apps().get(1)).events().getFirst())
                .satisfies(event -> {
                    assertThat(event.title()).isEqualTo("Afspraak");
                    assertThat(event.start()).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 0));
                    assertThat(event.location()).isNull();
                    assertThat(event.attendees()).isEmpty();
                    assertThat(event.notes()).isNull();
                    assertThat(event.lock().hint()).isEqualTo("Het wapen");
                });
    }

    @Test
    void from_givenLockedPhotoAndPage_expectTitlesWithoutDescriptionOrContent() {
        DesktopResponse response = DesktopResponse.from(DESKTOP, progress("desktop:computer"));

        assertThat(((PhotosAppResponse) response.apps().get(2)).photos().getFirst())
                .satisfies(photo -> {
                    assertThat(photo.title()).isEqualTo("Portret");
                    assertThat(photo.description()).isNull();
                });
        assertThat(((BrowserAppResponse) response.apps().get(3)).pages().getFirst())
                .satisfies(page -> {
                    assertThat(page.url()).isEqualTo("https://www.zoeken.nl/?q=gif");
                    assertThat(page.content()).isNull();
                });
    }

    @Test
    void from_givenUnlockedEventPhotoAndPage_expectDetails() {
        DesktopResponse response = DesktopResponse.from(
                DESKTOP,
                progress(
                        "desktop:computer",
                        "item:computer/agenda/notaris",
                        "item:computer/fotos/portret",
                        "item:computer/browser/zoekopdracht"));

        assertThat(((CalendarAppResponse) response.apps().get(1))
                        .events()
                        .getFirst()
                        .notes())
                .isEqualTo("Testament wijzigen.");
        assertThat(((PhotosAppResponse) response.apps().get(2))
                        .photos()
                        .getFirst()
                        .description())
                .isEqualTo("Een man met een kandelaar.");
        assertThat(((BrowserAppResponse) response.apps().get(3))
                        .pages()
                        .getFirst()
                        .content())
                .isEqualTo("Waar koop ik gif?");
    }

    private static TeamProgress progress(String... unlockedTargets) {
        CaseDefinition caseDefinition =
                new CaseDefinition("demo", CaseFixtures.manifestWithSetting("Geheim"), List.of(), List.of(DESKTOP));
        return new TeamProgress(caseDefinition, Set.of(unlockedTargets));
    }
}
