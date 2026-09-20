#!/usr/bin/env bash
# Scaffolds a new application from the app-starter seed.
#
# It does the mechanical half — copy, rename, re-point — and stops there. The judgement half
# (what the app is, what its entities are, what CLAUDE.md should say) is deliberately left to
# whoever runs it, because a generated answer to those is worse than an empty TODO.
#
#   scaffold.sh <app-name> <java-package> <prod-port> <stage-port> [target-dir]
#
# Example:
#   scaffold.sh inventory com.gftellez.inventory 8084 8085
#
# app-name     lowercase, digits and dashes — becomes the directory, the artifactId, the
#              service names and the database names.
# java-package dotted, lowercase — replaces com.example.app everywhere.
# ports        prod and staging. They must be free; the script refuses if either is in use.
set -euo pipefail

die() { echo "!! $*" >&2; exit 1; }

[[ $# -ge 4 && $# -le 5 ]] || die "usage: $0 <app-name> <java-package> <prod-port> <stage-port> [target-dir]"

APP="$1"; PKG="$2"; PROD_PORT="$3"; STAGE_PORT="$4"
TARGET="${5:-$HOME/dev/$APP}"

SEED_LOCAL="$HOME/dev/app-starter"
SEED_REMOTE="https://github.com/gftellez/app-starter.git"

[[ "$APP"  =~ ^[a-z][a-z0-9-]*$ ]] || die "app-name must be lowercase letters, digits and dashes: got '$APP'"
[[ "$PKG"  =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || die "java-package must be dotted lowercase: got '$PKG'"
[[ "$PROD_PORT"  =~ ^[0-9]{2,5}$ ]] || die "prod-port must be a number: got '$PROD_PORT'"
[[ "$STAGE_PORT" =~ ^[0-9]{2,5}$ ]] || die "stage-port must be a number: got '$STAGE_PORT'"
[[ "$PROD_PORT" != "$STAGE_PORT" ]] || die "prod and staging cannot share a port"
[[ -e "$TARGET" ]] && die "$TARGET already exists — refusing to write into it"

# A port collision surfaces as an app that "starts and then is not there", which is a bad
# afternoon. Check now, while it costs nothing.
if command -v ss >/dev/null 2>&1; then
  for p in "$PROD_PORT" "$STAGE_PORT"; do
    ss -ltnH "sport = :$p" | grep -q . && die "port $p is already in use on this machine"
  done
else
  echo "~~ ss not available; could not check whether ports $PROD_PORT/$STAGE_PORT are free"
fi

# ── the seed ────────────────────────────────────────────────────────────────────────────
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

if [[ -d "$SEED_LOCAL/.git" ]]; then
  echo "── seeding from $SEED_LOCAL"
  git -C "$SEED_LOCAL" archive HEAD | tar -x -C "$WORK"
else
  echo "── cloning the seed from GitHub"
  git clone --depth 1 -q "$SEED_REMOTE" "$WORK/clone"
  (cd "$WORK/clone" && git archive HEAD | tar -x -C "$WORK")
  rm -rf "$WORK/clone"
fi

[[ -f "$WORK/pom.xml" ]] || die "the seed does not look like app-starter (no pom.xml)"

# The scaffolder itself stays in the seed; a new app has no use for it.
rm -rf "$WORK/skill"

mkdir -p "$(dirname "$TARGET")"
cp -a "$WORK" "$TARGET"

# ── the package move ────────────────────────────────────────────────────────────────────
PKG_PATH="${PKG//.//}"
for root in main test; do
  src="$TARGET/src/$root/java/com/example/app"
  [[ -d "$src" ]] || continue
  dest="$TARGET/src/$root/java/$PKG_PATH"
  mkdir -p "$(dirname "$dest")"
  mv "$src" "$dest"
  # Leave no empty com/example behind.
  find "$TARGET/src/$root/java/com" -type d -empty -delete 2>/dev/null || true
done

# ── the renames ─────────────────────────────────────────────────────────────────────────
# Text files only: the seed carries no binaries, but a future one might.
mapfile -t FILES < <(find "$TARGET" -type f -not -path '*/.git/*')

# Derived names, kept in one place so they stay consistent across pom, scripts and units.
ARTIFACT_JAR="$APP-0.0.1-SNAPSHOT.jar"
DB_NAME="${APP//-/_}"
GROUP_ID="${PKG%.*}"     # com.gftellez.inventory -> com.gftellez

for f in "${FILES[@]}"; do
  sed -i \
    -e "s|com\.example\.app|$PKG|g" \
    -e "s|com/example/app|$PKG_PATH|g" \
    -e "s|app-starter-frontend|$APP-frontend|g" \
    -e "s|app-starter|$APP|g" \
    "$f"
done

# pom: the coordinates the blanket replace cannot get right on its own.
python3 - "$TARGET/pom.xml" "$GROUP_ID" "$APP" <<'PY'
import re, sys
path, group, app = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(path).read()
# Only the project's own coordinates — the parent block above must not be touched.
s = s.replace("""    <groupId>com.example</groupId>""", f"""    <groupId>{group}</groupId>""", 1)
s = re.sub(r"<description>.*?</description>",
           f"<description>TODO — what {app} is, in one line</description>", s, count=1)
s = s.replace("""    <!-- RENAME ME: groupId, artifactId, name and description are the four things a new
         app changes first. The package under src/main/java must follow. -->\n""", "")
open(path, "w").write(s)
PY

# deploy/config.sh — the single place the scripts read names from.
cat > "$TARGET/deploy/config.sh" <<CONFIG
# Sourced by the deploy scripts. Everything that differs per app lives here.
ARTIFACT=$ARTIFACT_JAR

STAGE_JAR=$APP-stage.jar
PROD_JAR=$APP-prod.jar

STAGE_SERVICE=$APP-demo.service
PROD_SERVICE=$APP.service

# The system java is usually older than the app needs; point at the JDK that builds it.
JDK_HOME=\${JDK_HOME:-$HOME/dev/.jdks/jdk-25.0.4.1+1}
MVN=\${MVN:-mvn}
CONFIG

# Ports, database names and paths in the environment files and the units.
sed -i -e "s|SERVER_PORT=8082|SERVER_PORT=$PROD_PORT|" \
       -e "s|/5432/app|/5432/$DB_NAME|" \
       -e "s|=app_user|=${DB_NAME}_user|" \
       -e "s|=app_migrator|=${DB_NAME}_migrator|" \
       "$TARGET/deploy/app.env.example" "$TARGET/.env.example"

sed -i -e "s|SERVER_PORT=8083|SERVER_PORT=$STAGE_PORT|" \
       -e "s|/5432/app_demo|/5432/${DB_NAME}_demo|" \
       -e "s|=app_demo_user|=${DB_NAME}_demo_user|" \
       -e "s|=app_demo_migrator|=${DB_NAME}_demo_migrator|" \
       "$TARGET/deploy/demo.env.example"

sed -i -e "s|127\.0\.0\.1:8082|127.0.0.1:$PROD_PORT|" \
       -e "s|app\.example\.com|$APP.example.com|g" \
       "$TARGET/deploy/nginx/site.conf"

# The unit files are named for the app, since two apps on one machine share /etc/systemd.
mv "$TARGET/deploy/systemd/app.service"      "$TARGET/deploy/systemd/$APP.service"
mv "$TARGET/deploy/systemd/app-demo.service" "$TARGET/deploy/systemd/$APP-demo.service"

sed -i -e "s|/home/app/dev/app|$TARGET|g" \
       -e "s|^User=app$|User=$USER|" \
       -e "s|Description=App |Description=$APP |" \
       -e "s|app-prod\.jar|$APP-prod.jar|" \
       -e "s|app-stage\.jar|$APP-stage.jar|" \
       "$TARGET/deploy/systemd/$APP.service" "$TARGET/deploy/systemd/$APP-demo.service"

sed -i -e "s|/home/app/dev/app|$TARGET|g" \
       -e "s|\bapp\.service|$APP.service|g" \
       -e "s|app-prod\.jar|$APP-prod.jar|g" \
       -e "s|/home/app/dev|$HOME/dev|g" \
       "$TARGET/deploy/systemd/README.md"

# The seed's README describes the seed. The new app gets a stub to write for itself.
cat > "$TARGET/README.md" <<README
# $APP

TODO — what this is.

Built from [app-starter](https://github.com/gftellez/app-starter). \`CLAUDE.md\` holds the
conventions and the reasons behind them; \`deploy/README.md\` holds the release flow.

\`\`\`bash
mvn clean package -DskipTests -B
sg docker -c 'mvn test'
cd frontend && npm run dev
\`\`\`
README

# ── the repository ──────────────────────────────────────────────────────────────────────
cd "$TARGET"
git init -q -b master
git add -A

cat <<DONE

── $APP scaffolded at $TARGET ──

Renamed: package $PKG, artifact $APP, ports $PROD_PORT (prod) / $STAGE_PORT (staging),
databases $DB_NAME and ${DB_NAME}_demo. Files are staged, nothing is committed yet.

Left for a person, on purpose:
  1. CLAUDE.md — every TODO in it is a question only you can answer.
  2. Delete the example once there are real entities: the Note entity, its repository and
     controller, its table in V1__baseline.sql, and TenantStampIntegrationTest. Keep the
     shape; write the same test for whatever replaces it.
  3. cp .env.example .env and fill it in.

Then verify before committing — the seed is known to build, so a failure here is the rename:
  JAVA_HOME=\$JDK_HOME mvn -B clean test
  (cd frontend && npm install && npm run build)
DONE
