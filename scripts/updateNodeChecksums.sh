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

# unofficial-builds publishes SHASUMS256.txt before every build has finished
# and appends to it as builds complete, so refuse to store a file that lacks
# an archive Flow downloads. Keep this list in sync with
# NodeInstallerTest.defaultNodeChecksums_coverDefaultVersionOnSupportedPlatforms.
missing=()
for archive in darwin-arm64.tar.gz darwin-x64.tar.gz linux-arm64.tar.gz \
  linux-x64.tar.gz win-arm64.zip win-x64.zip; do
  grep -q "  node-$version-$archive\$" "$work/SHASUMS256.txt" \
    || missing+=("$official/node-$version-$archive")
done
for archive in linux-arm64-musl.tar.gz linux-x64-musl.tar.gz; do
  grep -q "  node-$version-$archive\$" "$work/unofficial-SHASUMS256.txt" \
    || missing+=("$unofficial/node-$version-$archive")
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "Not all Node.js $version builds are published yet. Missing checksums for:" >&2
  printf '  %s\n' "${missing[@]}" >&2
  echo "Track the unofficial builds at https://unofficial-builds.nodejs.org/ and rerun this script when they are done." >&2
  exit 1
fi

cp "$work/SHASUMS256.txt" "$resources/default-node-SHASUMS256.txt"
cp "$work/unofficial-SHASUMS256.txt" "$resources/default-node-unofficial-SHASUMS256.txt"

echo "Wrote the Node.js $version checksums from $official and $unofficial to $resources"
