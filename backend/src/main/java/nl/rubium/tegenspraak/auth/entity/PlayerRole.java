package nl.rubium.tegenspraak.auth.entity;

/** There is exactly one ADMIN account, created at startup. Every other account is a PLAYER. */
public enum PlayerRole {
    PLAYER,
    ADMIN
}
