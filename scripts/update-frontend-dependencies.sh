#!/bin/bash

scriptDir=$(dirname $0)
depsFolder=$scriptDir/../flow-server/src/main/resources/com/vaadin/flow/server/frontend/dependencies

ncuVersion=22.2.3
# Skip versions younger than this. Must be at least the minimum frontend
# package age that Flow enforces on npm install (Options default: 1 day),
# or the created PR fails its first build because npm refuses to install
# the too-new package.
cooldown=1

# Get current git branch
currentBranch=$(git rev-parse --abbrev-ref HEAD)

# Determine update strategy based on branch
if [ "$currentBranch" = "main" ]; then
  updateTarget="latest"
else
  updateTarget="minor"
fi

echo "Current branch: $currentBranch"
echo "Update strategy: $updateTarget"

for a in "$depsFolder"/*
do
  pushd $a
  npx npm-check-updates@$ncuVersion -u -t "$updateTarget" --cooldown $cooldown
  popd
done

# Check for deprecated packages after updates
echo ""
echo "Checking for deprecated packages..."
deprecationFound=false

for a in "$depsFolder"/*
do
  pushd $a > /dev/null
  folderName=$(basename "$a")

  # Run npm-check-updates with --no-deprecated to find deprecated packages
  deprecatedOutput=$(npx npm-check-updates@$ncuVersion --no-deprecated --cooldown $cooldown 2>&1)

  # Check if there are any deprecated packages (output will contain package names if found)
  if echo "$deprecatedOutput" | grep -q "deprecated"; then
    echo "❌ Deprecated packages found in $folderName:"
    echo "$deprecatedOutput"
    deprecationFound=true
  else
    echo "✓ No deprecated packages in $folderName"
  fi

  popd > /dev/null
done

if [ "$deprecationFound" = true ]; then
  echo ""
  echo "ERROR: Deprecated packages detected!"
  echo "On maintenance branches, deprecated packages cannot be auto-updated to major versions."
  echo "Please review and update these packages manually."
  exit 1
fi
