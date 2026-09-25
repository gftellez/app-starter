# CLAUDE.md

Guidance for Claude Code working in this repository.

> **This file is a template.** Every `TODO` below is a question a new app has to answer.
> Delete what does not apply — a document that describes an app that does not exist is worse
> than a short one. Write it so a future session can act on it without asking: prose over
> bullet fragments, and every rule carries the reason it exists.

## Project overview

TODO — what this app is, in three or four sentences: what it does, who uses it, and what it
talks to (a database, an LLM, a messaging platform, a payment provider). Name the tenant:
whose data is this, and what is the thing a mistake here would cost?

## Build & run

```bash
mvn clean package -DskipTests -B      # build (the React build runs inside it)
sg docker -c 'mvn test'               # tests — Testcontainers needs Docker
mvn test -Dtest=ClassName             # one class
java -jar target/<artifact>.jar       # run, with a database already up
```

**Build with `clean`.** Without it, stale resources survive in `target/classes` — a rollback
built this way once shipped the very migrations it was rolling back.

**Requires JDK 25.** If the system `java` is older, build with
`JAVA_HOME=<path-to-jdk-25> mvn package`. TODO — record the path on this machine.

Frontend alone:

```bash
cd frontend && npm run dev      # Vite, proxying /api to the backend port
cd frontend && npm run build    # tsc --noEmit first, so a type error fails the jar build
```

## Architecture

TODO — the request flow, end to end, in numbered steps. Then the domain model as a table:
entity, key fields, and what it means. Someone should be able to read this section and know
where to put a new feature.

### Conventions that are easy to break

These are here because breaking them produces a bug that does not look like the mistake.

- **`spring.jpa.open-in-view` is off.** Reading a lazy association in a controller with no
  transaction throws, and the endpoint answers 500. Fix it with `@Transactional(readOnly =
  true)` on the read, or an `@EntityGraph` on the query when a slow call follows and you do
  not want to hold a database connection across it. Turning open-in-view on is never the fix.
- **Every owned entity carries the tenant.** `TenantOwned` plus
  `@EntityListeners(TenantStamper.class)`, so the column is filled on insert and cannot be
  forgotten. The stamp puts the row in the right place; only a **scoped query** keeps one
  tenant from reading another's. `TenantContext` refuses to guess once a second tenant
  exists — that refusal is deliberate, do not paper over it with a default.
- **Isolation is application-level, not enforced by the database.** Row-level security is
  the real fix; until then a query that forgets its filter reads everything.
- **Navigable state lives in the URL**, not in React state, so the back button, a refresh
  and a shared link all work (`frontend/src/lib/urlState.ts`). Defaults are omitted.
- **Dialogs must be portalled to `document.body`.** Every view sits inside a transformed
  element, and a transformed ancestor becomes the containing block for `position: fixed` — a
  dialog rendered inside a view pins itself to the scrolled content box and opens off-screen,
  backdrop and all.
- **Colours come from theme tokens**, never hard-coded, or one of the two themes breaks.
- **Query keys carry every parameter the query depends on**, and a mutation invalidates what
  it changed — otherwise the screen shows stale data and lies convincingly.
- **Every lookup by an id from a request goes through `CurrentTenant.owns`** (see
  `NoteController.get`). An id is a string the caller chose; without the check one tenant reads or
  edits another's row. Answer 404, not 403 — from there, it does not exist.
- **Outbound HTTP goes through the injected `RestClient.Builder`** (`HttpClientConfig`), which has a
  connect and a read timeout. Never `.block()` or call out without one, and **never call out from
  inside a `@Transactional` method**: a hung upstream then holds a request thread *and* a database
  connection, and a handful of those take the whole app down.
- **Webhooks are refused unless signed.** Verify the provider's signature over the **raw request
  body** (take `@RequestBody String`, not a parsed map), compare in constant time
  (`MessageDigest.isEqual`), reject stale timestamps, and treat a *missing* signature as a failure —
  not as a reason to skip the check. Read the provider's docs for the exact header: a real app
  verified a header its provider never sent, and so trusted every request.
- **Jackson 3 ignores fields it does not know**, silently. A client that sends `categoryName` to an
  endpoint expecting `categoryId` gets a 200 and no change. Keep write DTOs consistent with each
  other, or accept both spellings deliberately.
- **Things with history are archived, never deleted.** Deleting a row that others reference
  orphans them or quietly changes what past totals add up to.

## Tests

- Unit tests in `src/test/java/.../` — no database.
- Integration tests in `src/test/java/.../integration/`, extending `BaseIntegrationTest`,
  against a real PostgreSQL 16 in Testcontainers. The base class owns the container and
  truncates every table before each test, because a shared container otherwise leaks rows
  between classes.
- **Those tests are not transactional, on purpose.** A test wrapped in a transaction hides
  every lazy-loading bug; here a controller that reads a lazy association without one fails
  exactly as it would in a request.

TODO — list what each suite covers once there is something to list.

## Deployment

See `deploy/README.md` for the flow and the reasons. The short version: build to staging,
try it there, then promote the **same bytes** to production. Never rebuild for production,
never `cp` over a running jar, never share a jar between environments.

TODO — fill in the real table:

| Service | Port | Jar | Database | URL |
|---|---|---|---|---|
| | | | | |

## Configuration

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | the app's least-privilege database role |
| `FLYWAY_USER`, `FLYWAY_PASSWORD` | the role that owns the schema and runs migrations |
| `SERVER_PORT` | HTTP port |
| `APP_AUTH_TOKEN` | the shared token on the `X-Auth-Token` header |

TODO — add this app's own.

Secrets live in `.env` and `deploy/*.env`, which are git-ignored. Only the `*.example` files
are tracked. A token that reaches a tracked file has to be rotated, not deleted.

## Security notes

- `AuthFilter` is one shared token compared in constant time. It is a door lock, not
  sign-in: it proves the caller knows a secret, not who they are.
- The app refuses to start with the `changeme` placeholder token or database password
  (`DefaultSecretsCheck`): a missing env variable must be loud, not a guessable token in production.
- TODO — if this app sends model-generated input anywhere that executes it (SQL, a shell, an
  HTTP call), describe the guards here and treat that as the highest-risk surface in the
  repo. Anything a user or a document can write into a prompt is attacker-controlled text.

  What a real app on this stack learned the hard way, if it has an LLM agent:
  - **Never give the model a tool that writes SQL.** Text guards on SQL are pattern matches
    (`WHERE true` passed a "WHERE required" rule). Give it typed tools — `record_expense(account,
    amount, …)` — whose fields go through the same code path as the rest of the app.
  - **Run its read-only SQL inside a READ ONLY transaction**, so the database refuses a write
    that gets past the text check (a `SELECT` calling a writing function does).
  - **Tell the user what the tools did, not what the model says.** Replayed history teaches a
    model to answer "recorded" without calling anything; build the reply from the tools' own
    results, and challenge a claim of a write that no tool made.

## Working style

**Delegate to a subagent when the task reads a lot and returns a little** — repo-wide
searches, trawling build output, mechanical edits across many files. The agent absorbs that
output and only its summary comes back. **Do it inline when the task is small and precise**:
a subagent starts cold and re-derives the context first, so for a two-line fix delegating
costs more than it saves.

**Never delegate:** production data, deploys and service restarts, security changes, and
design calls someone is paying for judgement on.

When delegating to a general-purpose agent, say "do this yourself — do not spawn other agents":
one handed its task to sub-agents and came back with nothing but "waiting for them".

TODO — name this app's equivalent of "production data": the thing a cold agent must never
touch because the damage would be silent.
