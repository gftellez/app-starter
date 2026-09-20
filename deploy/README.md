# Deploying

Two environments on one machine: **staging** and **production**, each with its own
database, its own env file, its own jar and its own systemd unit. Staging exists to be the
place a release is tried before anyone's real data sees it.

```bash
# 1. build and put it on staging
deploy/deploy-stage.sh

# 2. try it there — really try it, not just "does it start"

# 3. promote the exact bytes that were tried
deploy/promote.sh
```

Set the names in `deploy/config.sh` first; the scripts read everything from there.

## The three rules, and what they cost to learn

**Promote, never rebuild.** `promote.sh` copies the staging jar to production. A rebuild is
a different artifact, so whatever staging proved, it did not prove about the thing you
shipped.

**`mv`, never `cp`, over a jar a service is running.** Boot loads classes lazily: writing
into the live JVM's file kills it with `ZipException: invalid stored block lengths` the
moment it needs a class it had not read yet. A rename swaps the directory entry and leaves
the running process on its old inode. Both scripts do this; do it by hand the same way.

**Never share a jar between environments.** When two services run one file, a deploy to one
takes down the other, and you find out from the wrong environment.

## Databases

`setup-db.sh` creates the migrator role, hands it the schema, and grants the app role rows
but not structure — including default privileges, so a table created by a future migration
is readable without anyone remembering to grant it.

```bash
sudo bash deploy/setup-db.sh app      app_user      .env
sudo bash deploy/setup-db.sh app_demo app_demo_user deploy/demo.env
```

A migration that fails with *must be owner of table …* is being run with the app's
credentials instead of the migrator's.

## Secrets

`.env`, `deploy/app.env` and `deploy/demo.env` are git-ignored and `chmod 600`. Only the
`*.example` files are tracked. If a token ever reaches a tracked file, rotate it — removing
the line is not enough once it has been pushed.
