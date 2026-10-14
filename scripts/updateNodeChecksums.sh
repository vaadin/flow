#!/bin/bash
#
# Downloads the official SHA-256 checksums that Flow verifies a downloaded
# Node.js archive against when it installs FrontendTools.DEFAULT_NODE_VERSION.
#
# Run this whenever DEFAULT_NODE_VERSION changes:
#
#   scripts/updateNodeChecksums.sh
#
# SHASUMS256.txt is stored exactly as published on nodejs.org, and only after
# its GPG signature verifies against the Node.js release keys, so the shipped
# file can be checked by comparing it with the published one.

set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
target="$root/flow-build-tools/src/main/resources/com/vaadin/flow/server/frontend/installer/default-node-SHASUMS256.txt"

version="v$("$root/scripts/frontendToolVersions.sh" | sed -n 's/^node-version=//p')"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

dist="https://nodejs.org/dist/$version"

curl -sSf -o "$work/SHASUMS256.txt" "$dist/SHASUMS256.txt"
curl -sSf -o "$work/SHASUMS256.txt.sig" "$dist/SHASUMS256.txt.sig"
curl -sSfL -o "$work/pubring.kbx" \
  https://github.com/nodejs/release-keys/raw/HEAD/gpg-only-active-keys/pubring.kbx
gpgv --keyring "$work/pubring.kbx" "$work/SHASUMS256.txt.sig" "$work/SHASUMS256.txt"

cp "$work/SHASUMS256.txt" "$target"

echo "Wrote the Node.js $version checksums from $dist/SHASUMS256.txt to $target"
