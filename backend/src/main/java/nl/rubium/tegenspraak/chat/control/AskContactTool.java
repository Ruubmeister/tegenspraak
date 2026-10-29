package nl.rubium.tegenspraak.chat.control;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * Tool the character's LLM calls when it asks another character something for the team. One instance per message: it
 * only collects the questions, the caller stores them once the reply succeeded, so a failed LLM call asks nothing.
 */
final class AskContactTool implements Function<AskContactTool.Request, String> {

    static final String NAME = "ask_contact";

    /** The question is written by the LLM; this keeps a runaway question out of the contact's prompt. */
    static final int MAX_QUESTION_LENGTH = 1000;

    record Request(String consultationId, String question) {}

    record AskedQuestion(ConsultationContact contact, String question) {}

    private final Map<String, ConsultationContact> askableById;
    private final List<AskedQuestion> asked = new ArrayList<>();

    AskContactTool(List<ConsultationContact> askable) {
        this.askableById = askable.stream()
                .collect(Collectors.toMap(
                        contact -> contact.consultation().id(),
                        contact -> contact,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    /** Without anyone to ask, the tool is not offered to the LLM. */
    boolean canAsk() {
        return !askableById.isEmpty();
    }

    ToolCallback toCallback() {
        return FunctionToolCallback.builder(NAME, this)
                .description("Stuur een bericht aan iemand die je kent, om iets voor de rechercheurs uit te zoeken."
                        + " Gebruik dit alleen als aan de voorwaarde voor dat onderwerp is voldaan. Geef het id van"
                        + " het onderwerp mee en de vraag die je stelt. Het antwoord komt pas na een paar minuten.")
                .inputType(Request.class)
                .build();
    }

    /** The result text goes back to the LLM, which then writes its reply. Only the first question per id counts. */
    @Override
    public synchronized String apply(Request request) {
        ConsultationContact contact = request == null ? null : askableById.get(request.consultationId());
        if (contact == null) {
            return "Onbekend id. Je kunt alleen over deze onderwerpen iets vragen: "
                    + String.join(", ", askableById.keySet());
        }
        if (request.question() == null || request.question().isBlank()) {
            return "Geef ook de vraag mee die je aan " + contact.contact().name() + " stelt.";
        }
        boolean alreadyAsked =
                asked.stream().anyMatch(question -> question.contact().equals(contact));
        if (!alreadyAsked) {
            asked.add(new AskedQuestion(contact, truncated(request.question().strip())));
        }
        return "Je bericht aan " + contact.contact().name() + " is verstuurd. Het antwoord komt pas over een paar"
                + " minuten, je weet het nu nog niet. Vertel de rechercheurs dat je het navraagt en erop terugkomt.";
    }

    synchronized List<AskedQuestion> asked() {
        return List.copyOf(asked);
    }

    private static String truncated(String question) {
        return question.length() <= MAX_QUESTION_LENGTH ? question : question.substring(0, MAX_QUESTION_LENGTH);
    }
}
