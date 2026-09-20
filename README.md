# app-starter

A seed for a Spring Boot 4 + React application: the parts that are the same every time, with
the reasons attached.

It is a **copy-and-rename** starter, not a library. Two apps do not justify a shared
dependency, and most of what repeats here is configuration and process rather than code —
things a library cannot carry anyway.

## What is in it

| | |
|---|---|
| `pom.xml` | Java 25, Boot 4, Flyway's three artifacts, Lombok as an explicit processor path, the React build wired into `package` |
| `deploy/` | two environments, the promote-don't-rebuild flow, the two database roles, systemd units, an nginx site |
| `src/main/java/.../tenant/` | the multi-tenant pattern: an interface, a stamping listener, a context that refuses to guess |
| `src/main/java/.../config/AuthFilter.java` | one shared token, compared in constant time |
| `src/main/java/.../error/` | exceptions to JSON, with server errors kept vague on purpose |
| `src/test/.../BaseIntegrationTest.java` | Testcontainers against a real PostgreSQL, non-transactional on purpose |
| `frontend/` | Vite + TS + Tailwind, URL-as-state, themed tokens, dialogs portalled to the body |
| `.claude/agents/` | ten subagents scoped to this stack |
| `CLAUDE.md` | the template a new app fills in |

## Starting a new app from it

The mechanical half is the `new-app` skill, installed globally at
`~/.claude/skills/new-app` so it works from any directory:

```bash
~/.claude/skills/new-app/scaffold.sh <name> <java-package> <prod-port> <stage-port>
```

It lives outside this repository on purpose — one copy, no drift between a tracked version
and an installed one. It is not backed up by git, so treat `~/.claude/skills/` as something
worth copying somewhere if the machine matters.

It copies the seed, moves the package, and re-points the pom, `deploy/config.sh`, the env
examples, the nginx site and the systemd units. It stops before anything that needs
judgement. By hand, after it:

1. Fill in `CLAUDE.md` — the `TODO` markers are the questions it asks.
2. Delete the example: `Note`, `NoteRepository`, `NoteController`, their table in
   `V1__baseline.sql`, and `TenantStampIntegrationTest`. Keep the shape; write the
   equivalent test for whatever replaces them.
3. `cp .env.example .env` and fill it in.
4. Verify: `mvn clean test` and `cd frontend && npm install && npm run build`.

## Its one weakness

A template drifts. This one stays true only if the next app is actually born from it and the
fixes come back. If you find yourself working around something here, fix it here.
