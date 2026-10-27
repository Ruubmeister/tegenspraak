package nl.rubium.tegenspraak.chat.control;

import nl.rubium.tegenspraak.casefile.entity.CharacterProfile;
import nl.rubium.tegenspraak.casefile.entity.ContactConsultation;

/** A consultation together with the character it asks. */
public record ConsultationContact(ContactConsultation consultation, CharacterProfile contact) {}
