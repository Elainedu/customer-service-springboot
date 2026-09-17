# customer-service-springboot

> Spring Boot REST backend + minimal chat UI for an e-commerce customer-service assistant,
> powered by a locally-hosted LLM (llama.cpp, OpenAI-compatible API) with per-session
> conversation history persisted in SQLite.

## What It Does

A small but end-to-end customer-service chatbot:

1. The browser opens the built-in `index.html` chat UI and generates a client-side `sessionId`.
2. Each user message is POSTed to `/api/chat` with `{sessionId, message}`.
3. The Spring Boot service loads all previous messages for that session from SQLite,
   prepends a Traditional-Chinese customer-service system prompt (`prompt.txt`),
   and calls a local LLM server (llama.cpp `/v1/chat/completions` — OpenAI-compatible).
4. The user turn and the assistant reply are saved back to the `conversations` table
   so the next call has full context.
5. The chat UI renders bubbles, a typing indicator, and quick-reply presets.

The assistant is scoped (via `prompt.txt`) to order lookup, returns / refunds, payment
issues, product enquiries, and member services for a Traditional-Chinese e-commerce site.

## System Architecture

```
 ┌─────────────────────┐        HTTP (JSON)         ┌────────────────────────┐
 │  Browser chat UI    │  ───────────────────────▶  │  Spring Boot (:8081)   │
 │  static/index.html  │                            │  ChatController        │
 │  - sessionId (uuid) │  ◀───────────────────────  │      │                 │
 │  - fetch /api/chat  │        ChatResponse        │      ▼                 │
 └─────────────────────┘                            │  ChatService           │
                                                    │   ├─ load history      │
                                                    │   ├─ build messages    │
                                                    │   ├─ save turn         │
                                                    │   └─ call LLM ─────────┼──┐
                                                    │                        │  │ HTTP
                                                    │  ConversationRepository│  │ POST
                                                    │       │  JPA/Hibernate │  │
                                                    │       ▼                │  ▼
                                                    │  ┌───────────────┐     │  ┌────────────────────┐
                                                    │  │  chat.db      │     │  │  llama.cpp server  │
                                                    │  │  (SQLite)     │     │  │  OpenAI-compat API │
                                                    │  │  conversations│     │  │  /v1/chat/         │
                                                    │  └───────────────┘     │  │   completions      │
                                                    └────────────────────────┘  └────────────────────┘
```

## Repository Layout

```
customer-service-springboot/
├── pom.xml                        # Maven build: Spring Boot 3.3.4, Java 21
├── chat.db                        # SQLite database file (auto-created / updated at runtime)
└── src/main/
    ├── java/com/example/customerservice/
    │   ├── CustomerServiceApplication.java   # @SpringBootApplication entry point
    │   ├── controller/
    │   │   └── ChatController.java           # REST endpoints under /api/chat
    │   ├── service/
    │   │   └── ChatService.java              # Core logic + llama.cpp client
    │   ├── repository/
    │   │   └── ConversationRepository.java   # Spring Data JPA repository
    │   ├── model/
    │   │   └── Conversation.java             # JPA @Entity → conversations table
    │   ├── dto/
    │   │   ├── ChatRequest.java              # {sessionId, message}
    │   │   └── ChatResponse.java             # {sessionId, answer, timestamp}
    │   └── exception/
    │       └── GlobalExceptionHandler.java   # 400 for validation, LLM-error pass-through
    └── resources/
        ├── application.properties            # port, DB, LLM base-url
        ├── prompt.txt                        # System prompt (Traditional-Chinese)
        └── static/index.html                 # Bundled chat UI (served at /)
```

## REST API

All endpoints are rooted at `/api/chat` (`ChatController`).

| Method | Path | Body / Params | Description |
|---|---|---|---|
| `POST` | `/api/chat` | `ChatRequest` — `{sessionId: string, message: string}` (both `@NotBlank`) | Sends a user turn. The service loads history for `sessionId`, calls the LLM, persists both turns, and returns `ChatResponse` = `{sessionId, answer, timestamp}`. |
| `GET`  | `/api/chat/history/{sessionId}` | Path variable `sessionId` | Returns `List<Conversation>` for that session, ordered by `createdAt` ascending — useful for re-hydrating the UI on page reload. |
| `GET`  | `/api/chat/sessions` | none | Returns `List<String>` — every distinct `sessionId` known to the database. Handy for building a "past conversations" list. |

Error responses:

- Validation failure (missing/blank `sessionId` or `message`) → `400 Bad Request`
  with body `{"error": "field: reason"}` (see `GlobalExceptionHandler`).
- LLM upstream 4xx/5xx → the same status code is proxied with body
  `{"error": "LLM API error: <status text>"}`.

## Database Schema

SQLite database `chat.db` (auto-created on first run via `spring.jpa.hibernate.ddl-auto=update`).
Single table derived from `Conversation` (`model/Conversation.java`):

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PRIMARY KEY AUTOINCREMENT | JPA `@GeneratedValue(IDENTITY)` |
| `session_id` | TEXT NOT NULL | Client-generated session id |
| `role` | TEXT NOT NULL | `"user"` or `"assistant"` |
| `content` | TEXT NOT NULL | Message body (long text supported) |
| `created_at` | DATETIME | Populated by `@PrePersist` = `LocalDateTime.now()` |

Because SQLite serialises writes, the Hikari pool is capped at
`spring.datasource.hikari.maximum-pool-size=1` to avoid `SQLITE_BUSY`.

## Tech Stack

- **Java 21** (see `pom.xml` `<java.version>`)
- **Spring Boot 3.3.4** — Web, Data JPA, Validation
- **SQLite** via `org.xerial:sqlite-jdbc:3.45.3.0`
- **Hibernate community dialect** (`SQLiteDialect`) for JPA on SQLite
- **Lombok** for `@Getter` / `@Setter` / `@RequiredArgsConstructor` / `@AllArgsConstructor`
- **Spring `RestClient`** to call the local LLM
- **Plain static HTML/CSS/JS** for the chat UI (`src/main/resources/static/index.html`) —
  no frontend framework, no Thymeleaf

## Configuration

All settings live in `src/main/resources/application.properties`:

```properties
# HTTP server (8081 chosen because llama.cpp defaults to 8080)
server.port=8081

# SQLite
spring.datasource.url=jdbc:sqlite:chat.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.datasource.hikari.maximum-pool-size=1

# Local LLM (OpenAI-compatible endpoint, e.g. llama.cpp server)
llm.base-url=http://192.168.31.250:8080
llm.model=local-model
```

To point at a different LLM host, override `llm.base-url` (e.g. `http://localhost:8080`).
The `llm.model` value is passed through to `/v1/chat/completions` but is ignored by
llama.cpp, so any non-empty string is fine.

### System prompt

Edit `src/main/resources/prompt.txt` to change tone / scope / rules. The file is
loaded once at startup via `@PostConstruct` (`ChatService.loadPrompt`), so changes
require a rebuild/restart.

## Building & Running

**Prerequisites:** JDK 21, Maven 3.9+, and a running LLM server exposing an
OpenAI-compatible `/v1/chat/completions` endpoint at `llm.base-url`.

### Dev run

```bash
mvn spring-boot:run
```

### Packaged jar

```bash
mvn clean package
java -jar target/customer-service-0.0.1-SNAPSHOT.jar
```

Then open `http://localhost:8081/` for the chat UI, or POST directly:

```bash
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"demo-1","message":"我的訂單什麼時候會到？"}'
```

### Running a local LLM

Any llama.cpp-compatible server works. Example:

```bash
llama-server -m <model.gguf> --port 8080 --host 0.0.0.0
```

Update `llm.base-url` in `application.properties` to match the server's address.

## What Is Not in the Repo

Per `.gitignore`:

- `target/`, `*.class`, `*.jar`, `*.war`, `*.log` — Maven build output
- `.mvn/`, `.classpath`, `.project`, `.settings/`, `.idea/`, `*.iml` — IDE / wrapper files
- `node_modules/`, `desktop.ini`, `.DS_Store` — OS / stray tool junk

Note: `chat.db` **is currently checked in** as a small seed database. If you want a
clean install, delete it before first run — Hibernate will recreate the schema.
Model weights for the LLM are not part of this repo (llama.cpp is an external process).

## License

Educational / personal use.
