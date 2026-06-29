#!/bin/bash
# Build a @since introduction index for one published artifact.
#
# Usage: build-index.sh <groupId> <artifactId> <indexOutDir> [minBaselineMajor]
#   <groupId>          e.g. com.vaadin
#   <artifactId>       e.g. flow-server  (must publish -sources.jar to Maven Central)
#   <indexOutDir>      directory where <artifactId>.tsv is written
#   [minBaselineMajor] optional; skip releases with major < this (e.g. 24)
#
# Indexes EVERY stable patch of every minor (plus, for the in-progress minor that
# has no GA yet, its latest pre-release). @since is then computed (apply step) as
# the start of the contiguous run of presence at full PATCH granularity reaching
# the latest release. That single rule handles everything without dates:
#  - a maintenance-release introduction -> X.Y.Z (it is continuously present from
#    there to now);
#  - a backport into an old maintenance patch is excluded, because the early
#    patches of the next minor (later in version order) lack it -> the streak
#    breaks before reaching the backport;
#  - a pre-release introduction -> bare minor X.Y.
#
# Extracted sources are cached under $SINCE_CACHE (default ~/.cache/since-tags),
# keyed by exact version (immutable), and reused across runs and repositories.
set -euo pipefail

# The host repo may export JAVA_TOOL_OPTIONS with a hotswap agent that crashes
# short-lived JVMs (jbang). Strip it for everything this script launches.
unset JAVA_TOOL_OPTIONS || true

GROUP="${1:?groupId}"; ART="${2:?artifactId}"; OUTDIR="${3:?indexOutDir}"; BASELINE="${4:-0}"
CACHE="${SINCE_CACHE:-$HOME/.cache/since-tags}"
HERE="$(cd "$(dirname "$0")" && pwd)"
BASE="https://repo1.maven.org/maven2/$(echo "$GROUP" | tr . /)/$ART"

ROOT="$CACHE/$GROUP/$ART"
SRC="$ROOT/src"
mkdir -p "$SRC" "$OUTDIR"

echo "[build-index] $GROUP:$ART  cache=$ROOT"

# 1. metadata -> the versions to index: every stable patch (baseline-filtered),
#    plus the latest pre-release of the highest minor if that minor has no GA yet.
curl -sf --max-time 60 "$BASE/maven-metadata.xml" -o "$ROOT/maven-metadata.xml"
allvers=$(grep -oE '<version>[0-9]+\.[0-9]+\.[0-9]+[^<]*</version>' "$ROOT/maven-metadata.xml" \
  | sed -E 's/<\/?version>//g' | awk -F. -v b="$BASELINE" '($1+0)>=(b+0)' | sort -V)
stable=$(echo "$allvers" | grep -vE '\-|\.(alpha|beta|rc)' || true)   # exclude pre-releases (both 25.2.0-rc2 and ancient 2.2.0.alpha11 styles)
tolist="$stable"
# in-progress minor: highest minor overall; if it has no stable release, add its latest pre-release
highminor=$(echo "$allvers" | awk -F. '{print $1"."$2}' | sort -V | tail -1)
if ! echo "$stable" | awk -F. -v t="$highminor" '($1"."$2)==t{f=1} END{exit !f}'; then
  prerel=$(echo "$allvers" | awk -F. -v t="$highminor" '($1"."$2)==t' | sort -V | tail -1)
  [ -n "$prerel" ] && tolist="$stable"$'\n'"$prerel"
fi
echo "[build-index] indexing $(echo "$tolist" | grep -c .) releases ($(echo "$tolist" | tail -1) latest)"

# 2. download + extract each (cache keyed by exact version)
echo "$tolist" | while read -r v; do
  [ -z "$v" ] && continue
  d="$SRC/$v"
  [ -f "$d/.ok" ] && continue
  rm -rf "$d"; mkdir -p "$d"
  if curl -sf --max-time 180 "$BASE/$v/$ART-$v-sources.jar" -o "$ROOT/s.jar"; then
    ( cd "$d" && unzip -oq "$ROOT/s.jar" ) && touch "$d/.ok" || { echo "[build-index] extract FAILED $v"; rm -rf "$d"; }
    rm -f "$ROOT/s.jar"
  else
    rm -rf "$d"; echo "[build-index] no sources for $v"
  fi
done

# 3. build the index over every cached version dir
jbang "$HERE/SinceTool.java" index "$SRC" "$OUTDIR/$ART.tsv"
