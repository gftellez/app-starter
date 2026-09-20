#!/usr/bin/env bash
# Build, then put the new jar on staging. Nothing here touches production.
#
# The rename is not a style choice. Boot loads classes lazily, so writing into the file a
# live JVM is executing kills it with "ZipException: invalid stored block lengths" the
# moment it needs a class it had not read yet. A rename swaps the directory entry and leaves
# the running process on its old inode.
#
# And the build is `clean package`: without clean, stale resources survive in target/classes
# — a rollback once shipped the very migrations it was rolling back.
set -euo pipefail

cd "$(dirname "$0")/.."
source deploy/config.sh

JAVA_HOME="$JDK_HOME" "$MVN" -B clean package "$@"

cp "target/$ARTIFACT" "deploy/$STAGE_JAR.new"
mv -f "deploy/$STAGE_JAR.new" "deploy/$STAGE_JAR"

kill "$(systemctl show "$STAGE_SERVICE" --property=MainPID --value)"
echo "Staging restarting. Shutdown is graceful and takes 10–15s — poll for a new PID and"
echo "an HTTP 200 before concluding anything failed."
