# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Friends and family of the maker, invited personally. There is no self-registration: the single admin creates every
player account. Players solve cases in casual sessions, alone (a team of one) or with others in a team they join with
a shareable code (`ABCD-EFGH`). Teammates may sit side by side or play remotely, and the team works on the same case
at the same time.

## Product Purpose

Tegenspraak is a Dutch detective game. A team takes on a case, questions its characters by chat and searches the
virtual desktops of people and organisations for evidence, then solves the case by answering its open questions in
their own words. Success is a team that cracks a case through its own questioning and deliberation, and wants to start
the next one.

## Positioning

- **Interrogation is real.** Characters are played by an LLM from a persona file and keep secrets. Nothing is unlocked
  by picking dialogue options: players have to ask, confront and catch contradictions ("tegenspraak") themselves.
- **Solving together.** Chats, unlocks and the verdict are shared per team and case. Characters see who is speaking
  (`[name]: ...`), so deliberation between detectives is part of play, not a side channel.

## Operating Context

- Flow: log in, pick or join a team, choose a case from the list, read the briefing, then switch freely between
  character chats, desktops and the verdict.
- Cases are YAML files with a title, summary, difficulty (`oefenzaak`, `makkelijk`, `gemiddeld`, `moeilijk`,
  `expert`), introduction, characters and desktops. Six cases exist today.
- Desktops are made of typed apps: mail, files, calendar, photos and browser. Items can be code-locked; a locked item
  shows only its title and a hint, and wrong codes trigger a cooldown.
- A message to a character can carry up to three opened desktop items as attachments.
- Characters can introduce new contacts and ask each other for information; such answers arrive a few minutes later.
- Teammates' activity arrives live; contacts with unread messages are marked per player.
- The verdict: typed answers to the case's questions, graded by an LLM against the expected answers, with a limited
  number of attempts per case (5 by default).
- The admin manages player accounts (create, reset password, delete); players change their own password.

## Capabilities and Constraints

- Desktop-first: laptops and desktops are the main setting, but the game must be fully playable on a phone.
- UI and game content are Dutch; code is English.
- Stack: React 19 + TypeScript + Vite frontend, Spring Boot backend, served from one origin behind nginx.
- Exactly one admin; admins cannot promote other accounts.
- Terminology: "team" (not group or room), "zaak"/case, "briefing", contacts/characters, desktops, verdict.

## Evidence on Hand

- Case content in `backend/src/main/resources/cases/` (six cases, characters and desktops).
- No testimonials, player counts, reviews or press exist. Do not fabricate any.

## Product Principles

1. The players do the detective work. The interface never solves, hints beyond what a case provides, or shortcuts the
   questioning.
2. Shared by default. Anything one detective discovers or unlocks belongs to the whole team.
3. The fiction carries the game. Desktops, mails and characters should feel like real people's material, in plain
   Dutch.
4. Quick to pick up for a casual circle: no onboarding burden, no accounts to manage for players.
