---
name: new-app
description: Start a new application from the app-starter seed — a Spring Boot 4 + React skeleton with the deploy kit, the tenant pattern and the subagents already in it. Use when the user wants to create a new app, start a new project, or scaffold a service on this stack.
---

# Starting a new app

`~/dev/app-starter` (github.com/gftellez/app-starter) is the seed. This skill copies it,
renames everything, and then hands back the parts that need a person.

Read `app-starter/README.md` before you start if you have not seen the seed before.

## 1. Ask for what only the user knows

Four answers, and you cannot guess any of them well:

| | |
|---|---|
| **name** | lowercase with dashes — becomes the directory, the artifact, the services, the databases |
| **Java package** | e.g. `com.gftellez.inventory` |
| **ports** | production and staging |
| **what the app is** | two or three sentences, for `CLAUDE.md` |

Ports in use on this machine: **8080** code-server, **8081** VoiceBoard backend, **8082**
finances prod, **8083** finances demo, **3001** VoiceBoard frontend. The script refuses a
port that is already listening, but it only sees what is running right now — check the
table in `~/dev/CLAUDE.md` too.

## 2. Run the script

```bash
~/.claude/skills/new-app/scaffold.sh <name> <java-package> <prod-port> <stage-port>
```

It copies the seed, moves the package, rewrites the pom coordinates, `deploy/config.sh`,
the env examples, the nginx site and the systemd units, then `git init` and stages
everything. It commits nothing — the first commit should include the work in step 3.

## 3. Do the part the script deliberately leaves

**Fill in `CLAUDE.md`.** Every `TODO` in it is a question. Answer them from what the user
told you, and ask when you do not know — an invented architecture section is worse than an
empty one, because the next session will believe it. Delete the TODOs that do not apply.

**Decide about the example.** `Note`, `NoteRepository`, `NoteController`, the `notes` table
in `V1__baseline.sql` and `TenantStampIntegrationTest` exist to show the tenant stamp
working end to end. Keep them until the app has a real entity, then delete all five and
write the equivalent stamp test for whatever replaced them — that test is the one thing in
the template that proves the mechanism still holds.

**Rename `Tenant`** if the app calls it something else — a workspace, a team, a clinic. The
mechanism matters, the word does not.

**`cp .env.example .env`** and fill it in. Never commit it.

## 4. Verify before committing

The seed is known to build, so anything failing here comes from the rename or from step 3:

```bash
JAVA_HOME=/home/finances/dev/.jdks/jdk-25.0.4.1+1 mvn -B clean test   # wrap in sg docker -c '…'
cd frontend && npm install && npm run build
```

Maven may not be on `PATH` — `/tmp/apache-maven-*/bin/mvn`.

Then commit. To put it on GitHub, the user creates the empty repo and you add the remote
using the token already embedded in another repo's remote URL; that token reaches any repo
on the account.

## 5. Afterwards

Add a row for the new project to the table in `~/dev/CLAUDE.md`.

**If you had to work around something in the seed, fix it in `app-starter` too.** A template
only stays true if the corrections come back to it; otherwise the next app starts from
something stale and everyone stops trusting it.

## What not to delegate

The scaffold itself is mechanical and fine to run inline. What a subagent must not do:
decide what the app is, write `CLAUDE.md` from guesses, or deploy anything. The ten
subagents come with the seed and are already scoped to this stack — `explorer` for search,
`backend-implementer` and `frontend-implementer` for work with weight in it, `qa` for the
suites.
