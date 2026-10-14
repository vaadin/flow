#!/bin/bash
#
# Regenerates the SHA-256 checksums that Flow verifies a downloaded Node.js
# archive against when it installs FrontendTools.DEFAULT_NODE_VERSION.
#
# Run this whenever DEFAULT_NODE_VERSION changes:
#
#   scripts/updateNodeChecksums.sh
#
# The official SHASUMS256.txt is only accepted if its GPG signature verifies
# against the Node.js release keys. Musl builds come from unofficial-builds,
# which publishes no signature, so those are fetched over HTTPS only.

set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
target="$root/flow-build-tools/src/main/resources/com/vaadin/flow/server/frontend/installer/default-node-SHASUMS256.txt"

version="v$("$root/scripts/frontendToolVersions.sh" | sed -n 's/^node-version=//p')"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

official="https://nodejs.org/dist/$version"
unofficial="https://unofficial-builds.nodejs.org/download/release/$version"

curl -sSf -o "$work/official.txt" "$official/SHASUMS256.txt"
curl -sSf -o "$work/official.txt.sig" "$official/SHASUMS256.txt.sig"
curl -sSfL -o "$work/pubring.kbx" \
  https://github.com/nodejs/release-keys/raw/HEAD/gpg-only-active-keys/pubring.kbx
gpgv --keyring "$work/pubring.kbx" "$work/official.txt.sig" "$work/official.txt"

curl -sSf -o "$work/unofficial.txt" "$unofficial/SHASUMS256.txt"

# Keep only the platform archives Flow downloads: node-<v>-<os>-<arch>.tar.gz
# or .zip from nodejs.org, and the -musl ones from unofficial-builds.
{
  grep -E "  node-$version-(darwin|linux|win)-[a-z0-9]+\.(tar\.gz|zip)$" "$work/official.txt"
  grep -E "  node-$version-linux-[a-z0-9]+-musl\.tar\.gz$" "$work/unofficial.txt"
} | sort -k2 > "$target"

echo "Wrote $(wc -l < "$target") checksums for Node.js $version to $target"
