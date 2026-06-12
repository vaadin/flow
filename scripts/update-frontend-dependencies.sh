#!/bin/bash

scriptDir=$(dirname $0)
depsFolder=$scriptDir/../flow-server/src/main/resources/com/vaadin/flow/server/frontend/dependencies

# Get current git branch
currentBranch=$(git rev-parse --abbrev-ref HEAD)

# Determine update strategy based on branch
if [ "$currentBranch" = "main" ]; then
  updateTarget="latest"
  commitMessage="chore: Bump frontend dependencies to latest versions"
else
  updateTarget="patch"
  commitMessage="chore: Bump frontend dependencies (patch releases only)"
fi

echo "Current branch: $currentBranch"
echo "Update strategy: $updateTarget"

for a in "$depsFolder"/*
do
  pushd $a
  npx npm-check-updates@18.1.1 -u -t "$updateTarget"
  popd
done

git add "$depsFolder"
git commit -m "$commitMessage"


