#!/usr/bin/env bash
# Promote the exact bytes that were tested on staging to production.
#
# It copies the staging jar; it never rebuilds. A rebuild produces a different artifact from
# the one that was actually tried, and then staging proved nothing.
set -euo pipefail

cd "$(dirname "$0")/.."
source deploy/config.sh
source deploy/wait-healthy.sh

if [[ ! -f "deploy/$STAGE_JAR" ]]; then
  echo "!! deploy/$STAGE_JAR does not exist — deploy to staging first" >&2
  exit 1
fi

read -r -p "Promote deploy/$STAGE_JAR to production? [y/N] " answer
[[ "$answer" == "y" || "$answer" == "Y" ]] || { echo "aborted"; exit 1; }

cp "deploy/$STAGE_JAR" "deploy/$PROD_JAR.new"
mv -f "deploy/$PROD_JAR.new" "deploy/$PROD_JAR"

restart_and_wait "$PROD_SERVICE" "$PROD_PORT"
