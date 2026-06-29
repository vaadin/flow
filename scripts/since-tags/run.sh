#!/bin/bash
# Reconcile @since tags for the whole repo (all flow-* modules) for a given target
# version. Builds the release index (unless SKIP_INDEX=1, when a valid cached index
# was restored), applies the tags, then formats only the files the tool touched.
#
# Usage: run.sh <DEV>            e.g. run.sh 24.10
#   SKIP_INDEX=1 run.sh <DEV>    reuse an existing .since-index/*.tsv (cache hit)
set -euo pipefail
unset JAVA_TOOL_OPTIONS || true

DEV="${1:?target version, e.g. 24.10}"
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
TOOL="$HERE/SinceTool.java"
IDX="$REPO/.since-index"
GROUP="com.vaadin"
ARTS="flow-server flow-data flow-html-components flow-polymer-template flow-dnd flow-webpush flow-react"
MVN="mvn"; [ -x "$REPO/mvnw" ] && MVN="$REPO/mvnw"

mkdir -p "$IDX"
cd "$REPO"

# 1. Build the index (per artifact; downloads cached under ~/.cache/since-tags).
if [ "${SKIP_INDEX:-0}" = "1" ] && ls "$IDX"/*.tsv >/dev/null 2>&1; then
  echo "[run] reusing cached index ($(ls "$IDX"/*.tsv | wc -l | tr -d ' ') tsv)"
else
  for a in $ARTS; do bash "$HERE/build-index.sh" "$GROUP" "$a" "$IDX"; done
fi

# 2. Apply per module.
for a in $ARTS; do
  [ -d "$a/src/main/java" ] || continue
  jbang "$TOOL" apply "$a/src/main/java" "$IDX" "$DEV" write "$IDX/report-$a.md"
done

# 3. Format only the files the tool changed (avoid reformatting unrelated files).
#    25.x uses spotless; 24.x and earlier use formatter-maven-plugin.
git diff --name-only -- '*.java' | sort > "$IDX/touched.txt"
if [ -s "$IDX/touched.txt" ]; then
  MODS="$(echo $ARTS | tr ' ' ,)"
  if grep -q spotless-maven-plugin pom.xml; then
    $MVN -q spotless:apply -pl "$MODS" || true
  else
    $MVN -q formatter:format -pl "$MODS" || true
  fi
  # revert any file the formatter touched that the @since tool did not
  git diff --name-only -- '*.java' | sort | comm -23 - "$IDX/touched.txt" | xargs -r git checkout --
fi

echo "[run] @since reconciliation done (DEV=$DEV)"
git --no-pager diff --shortstat -- '*.java' || true
