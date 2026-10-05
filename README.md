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

- `auth`: registration, session login/logout (Spring Security, BCrypt, CSRF via `XSRF-TOKEN` cookie)
- `casefile`: loads cases from YAML files at startup and exposes what the player may see
- `chat`: conversations with characters; the persona file becomes the LLM system prompt
- `desktop`: virtual desktops made of typed apps (`mail`, `files`)

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

Without `ANTHROPIC_API_KEY` everything works except character replies, which return a 502 and an error in the chat.
The model is configured in `backend/src/main/resources/application.yaml` (`spring.ai.anthropic.chat.model`).

## Writing a case

Cases live in `backend/src/main/resources/cases/`. Folder and file names are the ids (lowercase kebab-case):

```
cases/{case-id}/case.yaml                   title, summary, introduction, setting (hidden truth)
cases/{case-id}/characters/{id}.yaml        persona: name, role, description, personality, speechStyle,
                                            knownFacts, secrets (fact + revealWhen)
cases/{case-id}/desktops/{id}.yaml          ownerName, ownerType (person|organisation), description, apps
```

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
