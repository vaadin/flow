#!/bin/bash
#
# Prints the Node.js and pnpm versions Flow installs by default, in the
# `name=value` form GitHub Actions reads from $GITHUB_OUTPUT:
#
#   scripts/frontendToolVersions.sh >> "$GITHUB_OUTPUT"
#
# CI provisions the same versions a Flow application gets, so they are read
# from FrontendTools instead of being repeated in every workflow, where they
# drift apart whenever Flow moves to a new release.

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

echo "node-version=$node"
echo "pnpm-version=$pnpm"
