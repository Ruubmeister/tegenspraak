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
                                            knownFacts, secrets (fact + revealWhen)
cases/{case-id}/desktops/{id}.yaml          ownerName, ownerType (person|organisation), description, apps
```

The `solution` holds multiple choice `questions` (each with an `id`, `question`, `options` and the `answer` option
id) and an `epilogue`. The case is solved when every answer is correct; a wrong attempt only tells the player how
many answers were right. The epilogue is shown after solving, so it may reveal the whole truth.

Unknown properties or invalid ids fail startup, so mistakes are caught immediately. See `het-lek-bij-van-dijk` for an
example. Only a character's name, role and description are sent to the player; the setting and persona stay on the
server.

Adding a new desktop app type: add a record implementing `DesktopApp` (and register it in `@JsonSubTypes`), add the
matching type to `frontend/src/api/types.ts`, and render it in `DesktopAppView`.

## Checks

```bash
cd backend && ./mvnw verify          # Spotless format check, unit tests, integration tests (Testcontainers)
./mvnw spotless:apply                # format Java code

cd frontend
npm run lint && npm run format:check && npm test && npm run build
npm run format                       # format frontend code
```
