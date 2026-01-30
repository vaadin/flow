#!/bin/bash
#
# Installs git hooks from scripts/git-hooks/ to .git/hooks/
#
# Existing hooks are backed up to <hook-name>.local and will be
# called by the installed hooks.
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

# Only proceed if this is a git repository
if [ ! -d "$REPO_ROOT/.git/hooks" ]; then
    exit 0
fi

# Install pre-commit hook
HOOK_NAME="pre-commit"
SOURCE="$SCRIPT_DIR/git-hooks/$HOOK_NAME"
TARGET="$REPO_ROOT/.git/hooks/$HOOK_NAME"
BACKUP="$TARGET.local"

if [ -f "$SOURCE" ]; then
    # Backup existing hook if it differs from ours and isn't already backed up
    if [ -f "$TARGET" ] && [ ! -f "$BACKUP" ]; then
        if ! diff -q "$SOURCE" "$TARGET" > /dev/null 2>&1; then
            mv "$TARGET" "$BACKUP"
            echo "Existing $HOOK_NAME hook backed up to $BACKUP"
        fi
    fi

    cp "$SOURCE" "$TARGET"
    chmod +x "$TARGET"
fi
