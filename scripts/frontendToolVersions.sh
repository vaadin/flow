#!/bin/bash
#
# Prints the pnpm version Flow installs by default, in the `name=value` form
# GitHub Actions reads from $GITHUB_OUTPUT:
#
#   scripts/frontendToolVersions.sh >> "$GITHUB_OUTPUT"
#
# CI provisions the same versions a Flow application gets, so pnpm is read
# from FrontendTools instead of being repeated in every workflow, where it
# drifts apart whenever Flow moves to a new release. Node.js comes from the
# nodejs entry in .tool-versions, which the workflows hand to actions/setup-node
# as node-version-file; the script fails when that entry does not match
# FrontendTools, so the two cannot drift apart either.

set -euo pipefail

# FrontendTools moved from flow-server to flow-build-tools in 25.1, so the
# same script works on every maintenance branch.
root="$(dirname "$0")/.."
tools="$root/flow-build-tools/src/main/java/com/vaadin/flow/server/frontend/FrontendTools.java"
[ -f "$tools" ] || tools="$root/flow-server/src/main/java/com/vaadin/flow/server/frontend/FrontendTools.java"

node=$(sed -n 's/.*DEFAULT_NODE_VERSION = "v\{0,1\}\([0-9.]*\)".*/\1/p' "$tools")
pnpm=$(sed -n 's/.*DEFAULT_PNPM_VERSION = "\([0-9.]*\)".*/\1/p' "$tools")

if [ -z "$node" ] || [ -z "$pnpm" ]; then
  echo "::error::Could not read DEFAULT_NODE_VERSION and DEFAULT_PNPM_VERSION from $tools" >&2
  exit 1
fi

toolVersions="$root/.tool-versions"
ciNode=$(sed -n 's/^nodejs  *v\{0,1\}\([0-9.]*\) *$/\1/p' "$toolVersions")
if [ "$ciNode" != "$node" ]; then
  echo "::error file=.tool-versions::The nodejs version in .tool-versions (${ciNode:-missing}) does not match DEFAULT_NODE_VERSION in FrontendTools ($node)" >&2
  exit 1
fi

echo "pnpm-version=$pnpm"
