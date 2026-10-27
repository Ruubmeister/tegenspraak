package nl.rubium.tegenspraak.chat.control;

import java.util.List;

/**
 * What a character's prompt says about asking others: the consultations it may still start, and those it started and
 * is waiting on.
 */
public record ConsultationPrompt(List<ConsultationContact> askable, List<ConsultationContact> awaited) {

    public ConsultationPrompt {
        askable = List.copyOf(askable);
        awaited = List.copyOf(awaited);
    }

    public static ConsultationPrompt none() {
        return new ConsultationPrompt(List.of(), List.of());
    }
}
