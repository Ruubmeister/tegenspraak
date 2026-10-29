# Tegenspraak

A detective game. Players solve cases by chatting with characters (played by an LLM from a persona file) and searching
the virtual desktops of people and organisations for evidence. The game content and UI are in Dutch; code is in English.

## Structure

| Path              | What                                                                        |
| ----------------- | --------------------------------------------------------------------------- |
| `backend/`        | Spring Boot 4 (Java 21, Maven), Spring AI with Anthropic, Postgres + Flyway |
| `frontend/`       | React 19 + TypeScript + Vite, React Router, TanStack Query                  |
| `compose.yaml`    | Local Postgres, started automatically by the backend in development         |

The backend is a single service, split into feature packages that each follow boundary-control-entity:

- `auth`: session login/logout and own password change (Spring Security, BCrypt, CSRF via `XSRF-TOKEN` cookie);
  creates the admin account at startup
- `useradmin`: the admin creates players, resets their passwords and deletes them (`/api/admin/**`, ADMIN only)
- `casefile`: loads cases from YAML files at startup and exposes what the player may see
- `team`: players investigate in teams; others join with the team's code (`ABCD-EFGH`). Playing alone is a team of
  one, and the last member to leave deletes the team with its progress
- `chat`: a team's shared conversation with a character; the persona file becomes the LLM system prompt, and player
  messages reach the LLM as `[name]: ...` so the character can tell the detectives apart. A message can carry up to
  three opened desktop items (mails, files, appointments, photos, web pages) as attachments; only references are stored, and the character reads them in full.
  Characters can ask each other for information; the answer is scheduled and arrives a few minutes later. Each player
  has their own read state per conversation, so contacts with new messages are marked
- `desktop`: virtual desktops made of typed apps (`mail`, `files`, `calendar`, `photos`, `browser`)
- `investigation`: what a team can see and use in a case: introduced contacts, and desktops and their items opened
  with their code or by a contact's answer. Unlocks are shared by the team
- `liveupdate`: pushes "something changed" signals to the team's open browsers as server-sent events
- `verdict`: the team solves a case by typing answers to its open questions, which an LLM grades against the expected
  answers; a team gets a limited number of attempts per case (`tegenspraak.verdict.max-attempts`, 5)

## Running locally

Requirements: Java 21, Node 22, Docker.

```bash
# Backend on http://localhost:8081 (starts Postgres via compose.yaml)
cd backend
ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run

# Frontend on http://localhost:5173 (proxies /api to the backend)
cd frontend
npm install
npm run dev
```

## Running in production (Debian)

Production uses an existing Postgres server and no Docker; the packaged jar leaves out the Docker Compose support.
Example configs are in `deploy/`: a systemd unit for the backend and an nginx site that serves the frontend build and
proxies `/api` to the backend, so both share one origin. The event stream needs nothing extra: the backend turns off
nginx buffering for it with an `X-Accel-Buffering: no` header, and its heartbeat stays well within `proxy_read_timeout`.
Browsers allow only six HTTP/1.1 connections per site and every open tab keeps one stream open, so enable HTTP/2 once
HTTPS is set up (`listen 443 ssl; http2 on;`).

```bash
# Build
(cd backend && ./mvnw -DskipTests package)
(cd frontend && npm ci && npm run build)

# Backend
apt install openjdk-21-jre-headless nginx
useradd --system --home /opt/tegenspraak --shell /usr/sbin/nologin tegenspraak
install -D -m 644 backend/target/tegenspraak-0.0.1-SNAPSHOT.jar /opt/tegenspraak/tegenspraak.jar
install -D -m 600 deploy/systemd/tegenspraak.env.example /etc/tegenspraak/tegenspraak.env  # then edit it
cp deploy/systemd/tegenspraak-backend.service /etc/systemd/system/
systemctl daemon-reload && systemctl enable --now tegenspraak-backend

# Frontend
mkdir -p /var/www/tegenspraak && cp -r frontend/dist/. /var/www/tegenspraak/
cp deploy/nginx/tegenspraak.conf /etc/nginx/sites-available/  # then set server_name
ln -s /etc/nginx/sites-available/tegenspraak.conf /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

Create the database and its owner on the Postgres server first; Flyway creates the tables on the first startup. Logs
are in `journalctl -u tegenspraak-backend`.

## Accounts

There is no self registration. On the first startup the backend creates a single admin account (`admin`, see
`tegenspraak.admin` in `application.yaml`). Set `TEGENSPRAAK_ADMIN_PASSWORD` (8 to 72 characters) to choose its
password; without it a random password is generated and logged once at WARN level. The admin manages players under
"Gebruikersbeheer"; everyone can change their own password under "Mijn account". Deleting a player removes their team
memberships and deletes teams left without members; their messages stay in the chats of teams that remain. Deleting a
player or resetting their password ends their open sessions.

Chat and verdicts are shared within a team. Every open team or case page keeps a server-sent event stream open
(`GET /api/teams/{teamId}/events`). Once a change is committed, the backend sends the team a small event that only says
where to look (`CHAT` with case and character, `CASE`, `VERDICT` or `MEMBERS`); the frontend then reloads that data
through the regular endpoints, so the stream never carries content and needs no access checks of its own. The browser
reconnects by itself and reloads the team's data after a reconnect, and every view still polls once a minute in case
the stream is down. A heartbeat every 25 seconds keeps proxies from closing the stream and closes it once the player
left the team or the session ended; streams also end after 30 minutes, after which the browser reconnects
(`tegenspraak.live-updates`). The streams are kept in memory, which works because the backend runs as a single
instance.

A contact whose conversation has a reply or notice the player has not read gets a dot in the case navigation (and on
the menu button on small screens). An open conversation is marked read up to its newest message while the tab is
visible. Read state is per player, so a reply a teammate already read is still new for the others.

Messages to the same character within one team are handled one at a time, so the character always replies with the
whole conversation in view.

Without `ANTHROPIC_API_KEY` everything works except character replies, which return a 502 and an error in the chat.
The model is configured in `backend/src/main/resources/application.yaml` (`spring.ai.anthropic.chat.model`).

## Writing a case

Cases live in `backend/src/main/resources/cases/`. Folder and file names are the ids (lowercase kebab-case):

```
cases/{case-id}/case.yaml                   title, summary, difficulty, introduction, setting (hidden truth), solution
cases/{case-id}/characters/{id}.yaml        persona: name, role, description, personality, speechStyle,
                                            knownFacts, secrets (fact, revealWhen, requiresEvidence, minimumMood),
                                            hidden, introduces, startingMood, consults
cases/{case-id}/desktops/{id}.yaml          ownerName, ownerType (person|organisation), description, lock, apps
```

The `solution` holds open `questions` and an `epilogue`. The team types its answers; an LLM grades each one against
the question's `answer`, which the team never sees. Write the `answer` as the key points a correct answer must contain:
an answer that misses a key point, contradicts it or hedges ("A or B") is wrong.

```yaml
- id: opgesloten
  question: Waarom kwam Eva Brink de ruimte niet uit?
  answer: >-
    Kernpunten: de mechanische nachtgrendel was van buitenaf dichtgeschoven; daardoor opende de deur niet toen ze
    op de noodknop drukte.
```

The case is solved when every answer is correct. A wrong attempt only tells the team how many answers were right, not
which ones. After the last wrong attempt the case is failed. The epilogue is shown once the case is solved or failed, so
it may reveal the whole truth. A grading failure stores nothing, so it does not cost an attempt.

### Secrets that need evidence

A character gives up a secret once the `revealWhen` condition is met, as judged by its LLM. Players can bluff about
evidence they do not have, so a secret can also list the desktop items the team must actually attach in the
conversation with that character:

```yaml
secrets:
  - fact: Hij kwam om 21:48 terug in het gebouw.
    revealWhen: De rechercheurs confronteren hem met het toegangslog van de hoofdingang.
    requiresEvidence:
      - { desktop: politiedossier, app: dossier, item: toegang-hoofdingang }
    requiredCount: 1 # optional; without it every listed item is required
```

Until enough of the evidence has been shown (in any earlier message or the current one), the character's prompt only
tells it to deny the secret; the `revealWhen` condition is left out, so the LLM cannot be talked into it. Every listed
item must exist in the case, or startup fails.

### Hidden contacts

A character with `hidden: true` is not available until another character introduces it. The introducer lists it under
`introduces`, with the condition in the conversation:

```yaml
introduces:
  - character: dennis-de-vries
    when: Je hebt verteld dat Dennis bij Kramer Bouw werkt en de rechercheurs willen hem spreken.
```

The introducer's LLM gets an `introduce_contact` tool and calls it once the condition is met. The contact is then
available to the whole team, and a notice appears in the chat. Every hidden character needs at least one introducer,
or a consultation that unlocks it (see below).

### Asking other characters

A character can send another character a message on behalf of the team, for example to find out the code of a
machine. Only consultations listed in the case can happen:

```yaml
consults:
  - id: code-atelier-pauw # unique per character
    character: hendrik-pauw # the contact; may be hidden
    about: De code van de beveiligde pc in Atelier Pauw.
    when: De rechercheurs vragen of jij Hendrik kunt vragen hoe ze op zijn pc kunnen kijken.
    answer: De code van de pc in zijn atelier is "spieraam".
    minimumMood: trusting # optional, like introductions
    unlocks: # optional, opened for the team once the answer arrives
      - { character: luc-verstraeten } # a hidden character
      - { desktop: atelier-pauw } # a locked desktop
      - { desktop: atelier-pauw, app: werkorders, item: werkorder-kopieen } # a locked item
```

The character's LLM gets an `ask_contact` tool and calls it with its own question once `when` is met; a notice
appears in the chat. After a random delay (`tegenspraak.consultations`, 2 to 5 minutes) the contact's LLM answers,
then the asking character passes the answer on in a new chat message, and anything listed under `unlocks` opens with a
notice. The contact only gets its persona and the `answer`, not its secrets or the case setting, so the question
cannot get more out of it than the case author intended. The team sees the notices and the passed-on message, not the
contact's own reply.

Each consultation happens once per team. Due times are stored in the database, so pending answers survive a restart.
A failed answer is retried (3 attempts in total); after that the request is dropped with a notice and the character
can be asked again.

### Character mood

Every character keeps a hidden mood towards each team: `hostile`, `suspicious`, `neutral`, `friendly` or `trusting`.
It starts at `startingMood` (default `neutral`: polite but reserved, wondering what the team wants) and shifts with
the tone of every message, as judged by a small LLM (`tegenspraak.chat.tone-classifier-model`). Helping or convincing
the character makes it friendlier; accusing or aggressive messages make it like the team less. The character's prompt
describes its current mood, so it answers warmer or colder. Players never see the mood, only how the character
reacts.

A secret or introduction can also require a minimum mood. Until the character feels at least that good about the
team, its prompt only tells it to evade, and an introduction is not offered at all:

```yaml
startingMood: suspicious
secrets:
  - fact: Ze had schulden bij Kuiper.
    revealWhen: De rechercheurs vragen naar geld.
    minimumMood: friendly
introduces:
  - character: lisa-brink
    when: De rechercheurs vragen naar Eva's familie.
    minimumMood: trusting
```

A failed tone judgement counts as neutral, so it never blocks the chat.

### Locks

A desktop, a mail or a file can be locked with a code that the team has to work out:

```yaml
lock:
  code: "1403"
  hint: "Wachtwoordhint: etentje bij de ouders van D. (ddmm)"
```

Codes are compared ignoring case and whitespace. Players see the hint and, for items, the subject, name or title;
the content is only sent once the team entered the code. A locked desktop shows no apps at all. After 5 wrong codes
for one item within a minute the team has to wait (`tegenspraak.locks` in `application.yaml`). Items and apps
need ids that are unique within their parent, because locks are addressed by them.

Unknown properties or invalid ids fail startup, so mistakes are caught immediately. Only a character's name, role and
description are sent to the player; the setting, persona and lock codes stay on the server.

Every case sets a `difficulty` (`oefenzaak`, `makkelijk`, `gemiddeld`, `moeilijk` or `expert`). Players sort the case
list by difficulty (easiest first by default, or hardest first) or by title; cases of the same difficulty are sorted
by title. By convention the title also ends with the difficulty:

| Case                            | Difficulty | What makes it harder                                                                                 |
| ------------------------------- | ---------- | ---------------------------------------------------------------------------------------------------- |
| `het-lek-bij-van-dijk`          | oefenzaak  | Walkthrough: two characters, direct clues                                                            |
| `kas-van-vv-rijnvogels`         | makkelijk  | One red herring, one hidden witness, one lock with the code on another desktop                       |
| `kunstbrand-in-haarlem`         | gemiddeld  | A lying suspect, two hidden contacts, a lock code only a character knows                             |
| `laatste-experiment`            | moeilijk   | Several liars, a chain of introductions, a timeline to rebuild from logs                             |
| `puzzelmaker-van-deventer`      | expert     | A puzzle to solve in the game, consultations that unlock a desktop and a file, two suspects to clear |
| `testament-van-huize-wolfslaar` | expert     | Two convincing red herrings, mood-gated witnesses, chained lock codes, photo comparison              |

Adding a new desktop app type: add a record implementing `DesktopApp` (and register it in `@JsonSubTypes`), add a
matching response to `DesktopResponse` that leaves out locked content, add the type to `frontend/src/api/types.ts`,
and render it in `DesktopAppView`.

## Checks

```bash
cd backend && ./mvnw verify          # Spotless format check, unit tests, integration tests (Testcontainers)
./mvnw spotless:apply                # format Java code

cd frontend
npm run lint && npm run format:check && npm test && npm run build
npm run format                       # format frontend code
```
