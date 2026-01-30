#!/bin/bash
#
# Installs git hooks from scripts/git-hooks/ to .git/hooks/
#
# Existing hooks are backed up to <hook-name>.local and will be
# called by the installed hooks.
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
SOURCE="$SCRIPT_DIR/git-hooks/pre-commit"
TARGET="$REPO_ROOT/.git/hooks/pre-commit"

[ ! -d "$REPO_ROOT/.git/hooks" ] && exit 0
[ ! -f "$SOURCE" ] && exit 0

# Backup existing hook if it differs from ours and isn't already backed up
if [ -f "$TARGET" ] && [ ! -f "$TARGET.local" ]; then
    if ! diff -q "$SOURCE" "$TARGET" > /dev/null 2>&1; then
        mv "$TARGET" "$TARGET.local"
        echo "Existing pre-commit hook backed up to $TARGET.local"
    fi
fi

cp "$SOURCE" "$TARGET"
chmod +x "$TARGET"
