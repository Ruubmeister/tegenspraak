package nl.rubium.tegenspraak.chat.entity;

/** NOTICE messages are game events shown in the chat, such as an introduced contact. The LLM never sees them. */
public enum MessageSender {
    PLAYER,
    CHARACTER,
    NOTICE
}
