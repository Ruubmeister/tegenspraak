package nl.rubium.tegenspraak.chat.control;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Checks regularly for consultations that are due. Due times are stored in the database, so pending answers survive a
 * restart. The application runs as a single instance and the scheduler on a single thread, so a request is never
 * answered twice at the same time.
 */
@Component
class ConsultationScheduler {

    private final ConsultationService consultationService;

    ConsultationScheduler(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @Scheduled(
            initialDelayString = "${tegenspraak.consultations.poll-interval}",
            fixedDelayString = "${tegenspraak.consultations.poll-interval}")
    void answerDueConsultations() {
        consultationService.answerDueConsultations();
    }
}
