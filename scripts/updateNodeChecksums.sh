#!/bin/bash
#
# Downloads the SHA-256 checksums that Flow verifies a downloaded Node.js
# archive against when it installs FrontendTools.DEFAULT_NODE_VERSION.
#
# Run this whenever DEFAULT_NODE_VERSION changes:
#
#   scripts/updateNodeChecksums.sh
#
# Both SHASUMS256.txt files are stored exactly as published, so the shipped
# files can be checked by comparing them with the published ones. The one from
# nodejs.org is only stored after its GPG signature verifies against the
# Node.js release keys. The one from unofficial-builds, used for the musl
# builds, is not signed, so it is fetched over HTTPS only.

set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
resources="$root/flow-build-tools/src/main/resources/com/vaadin/flow/server/frontend/installer"

version="v$("$root/scripts/frontendToolVersions.sh" | sed -n 's/^node-version=//p')"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

official="https://nodejs.org/dist/$version"
unofficial="https://unofficial-builds.nodejs.org/download/release/$version"

curl -sSf -o "$work/SHASUMS256.txt" "$official/SHASUMS256.txt"
curl -sSf -o "$work/SHASUMS256.txt.sig" "$official/SHASUMS256.txt.sig"
curl -sSfL -o "$work/pubring.kbx" \
  https://github.com/nodejs/release-keys/raw/HEAD/gpg-only-active-keys/pubring.kbx
gpgv --keyring "$work/pubring.kbx" "$work/SHASUMS256.txt.sig" "$work/SHASUMS256.txt"
curl -sSf -o "$work/unofficial-SHASUMS256.txt" "$unofficial/SHASUMS256.txt"

cp "$work/SHASUMS256.txt" "$resources/default-node-SHASUMS256.txt"
cp "$work/unofficial-SHASUMS256.txt" "$resources/default-node-unofficial-SHASUMS256.txt"

echo "Wrote the Node.js $version checksums from $official and $unofficial to $resources"
