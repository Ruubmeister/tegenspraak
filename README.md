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
  messages reach the LLM as `[name]: ...` so the character can tell the detectives apart
- `desktop`: virtual desktops made of typed apps (`mail`, `files`)
- `investigation`: what a team can see and use in a case: introduced contacts and desktops, mails and files opened
  with their code. Unlocks are shared by the team
- `verdict`: the team solves a case by answering its multiple choice questions; attempts are stored per team

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

## Accounts

There is no self registration. On the first startup the backend creates a single admin account (`admin`, see
`tegenspraak.admin` in `application.yaml`). Set `TEGENSPRAAK_ADMIN_PASSWORD` (8 to 72 characters) to choose its
password; without it a random password is generated and logged once at WARN level. The admin manages players under
"Gebruikersbeheer"; everyone can change their own password under "Mijn account". Deleting a player removes their team
memberships and deletes teams left without members; their messages stay in the chats of teams that remain. Deleting a
player or resetting their password ends their open sessions.

Chat and verdicts are shared within a team. The frontend refreshes an open conversation every 3 seconds and an open
verdict every 5 seconds, so teammates see each other's messages and attempts. Messages to the same character within one
team are handled one at a time, so the character always replies with the whole conversation in view.

Without `ANTHROPIC_API_KEY` everything works except character replies, which return a 502 and an error in the chat.
The model is configured in `backend/src/main/resources/application.yaml` (`spring.ai.anthropic.chat.model`).

## Writing a case

Cases live in `backend/src/main/resources/cases/`. Folder and file names are the ids (lowercase kebab-case):

```
cases/{case-id}/case.yaml                   title, summary, introduction, setting (hidden truth), solution
cases/{case-id}/characters/{id}.yaml        persona: name, role, description, personality, speechStyle,
                                            knownFacts, secrets (fact + revealWhen), hidden, introduces
cases/{case-id}/desktops/{id}.yaml          ownerName, ownerType (person|organisation), description, lock, apps
```

The `solution` holds multiple choice `questions` (each with an `id`, `question`, `options` and the `answer` option
id) and an `epilogue`. The case is solved when every answer is correct; a wrong attempt only tells the player how
many answers were right. The epilogue is shown after solving, so it may reveal the whole truth.

### Hidden contacts

A character with `hidden: true` is not available until another character introduces it. The introducer lists it under
`introduces`, with the condition in the conversation:

```yaml
introduces:
  - character: dennis-de-vries
    when: Je hebt verteld dat Dennis bij Kramer Bouw werkt en de rechercheurs willen hem spreken.
```

The introducer's LLM gets an `introduce_contact` tool and calls it once the condition is met. The contact is then
available to the whole team, and a notice appears in the chat. Every hidden character needs at least one introducer.

### Locks

A desktop, a mail or a file can be locked with a code that the team has to work out:

```yaml
lock:
  code: "1403"
  hint: "Wachtwoordhint: etentje bij de ouders van D. (ddmm)"
```

Codes are compared ignoring case and whitespace. Players see the hint and, for mails and files, the subject or name;
the content is only sent once the team entered the code. A locked desktop shows no apps at all. After 5 wrong codes
for one item within a minute the team has to wait (`tegenspraak.locks` in `application.yaml`). Mails, files and apps
need ids that are unique within their parent, because locks are addressed by them.

Unknown properties or invalid ids fail startup, so mistakes are caught immediately. Only a character's name, role and
description are sent to the player; the setting, persona and lock codes stay on the server.

The case list is sorted by id. The title ends with the difficulty, so players can pick a case that suits them:

| Case                    | Difficulty  | What makes it harder                                                           |
| ----------------------- | ----------- | ------------------------------------------------------------------------------ |
| `het-lek-bij-van-dijk`  | oefenzaak   | Walkthrough: two characters, direct clues                                      |
| `kas-van-vv-rijnvogels` | makkelijk   | One red herring, one hidden witness, one lock with the code on another desktop |
| `kunstbrand-in-haarlem` | gemiddeld   | A lying suspect, two hidden contacts, a lock code only a character knows       |
| `laatste-experiment`    | moeilijk    | Several liars, a chain of introductions, a timeline to rebuild from logs       |

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
