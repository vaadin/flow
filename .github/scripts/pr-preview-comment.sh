#!/usr/bin/env bash
# Writes the preview comment of pr-preview.yml on the pull request, editing the
# one comment that carries $MARKER instead of adding another, so that a series
# of restarts does not bury the pull request in notifications.
#
#   pr-preview-comment.sh comment.md [owner-line]
#
# With an owner line, the comment is only edited while it still contains that
# line: a run that ends after a newer run took the comment over leaves it
# alone.
set -euo pipefail

body=$1
owner=${2:-}
repo=$GITHUB_REPOSITORY

# --jq runs once per page under --paginate, so each page yields at most one
# comment and the first of those is the one; the trimming is a parameter
# expansion rather than a pipe, so a failed listing still fails the script
# instead of reading as "none yet" and posting a second comment.
existing=$(gh api "repos/$repo/issues/$_PR_NUMBER/comments" --paginate \
  --jq "[.[] | select(.body | startswith(\"$MARKER\")) | {id, body}] | first // empty | @base64")
existing=${existing%%$'\n'*}

if [ -z "$existing" ]; then
  [ -z "$owner" ] || exit 0
  gh api "repos/$repo/issues/$_PR_NUMBER/comments" -F body=@"$body" > /dev/null
  exit 0
fi

id=$(printf '%s' "$existing" | base64 -d | jq -r .id)
if [ -n "$owner" ] && ! printf '%s' "$existing" | base64 -d | jq -r .body | grep -qF "$owner"; then
  exit 0
fi
gh api --method PATCH "repos/$repo/issues/comments/$id" -F body=@"$body" > /dev/null
