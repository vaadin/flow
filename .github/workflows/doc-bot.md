---
name: Documentation Bot
description: >
  Runs on every pull request and on every push to one, and keeps a draft
  documentation pull request in vaadin/docs in step with the change.

on:
  pull_request:
    types: [opened, reopened, synchronize, ready_for_review, assigned]
    # Free first filter: a pull request that touches none of these paths never
    # starts a runner, so the cheapest check happens before any tokens are
    # spent. GitHub skips path filtering above 300 changed files, which is why
    # Phase 1 classifies the files it sees rather than trusting this list.
    paths-ignore:
      - '**/src/test/**'
      - '**/src/it/**'
      - '**/*Test.java'
      - '**/*IT.java'
      - 'flow-tests/**'
      - '**/pom.xml'
      - '**/*.gradle*'
      - '**/package.json'
      - '**/package-lock.json'
      - '.github/**'
      - 'scripts/**'
      - '**/Dockerfile'
      - '**/*.md'

# Two ways in. Normally the bot decides for itself on every push to a
# non-draft pull request, skipping the conventional-commit types that never
# reach a reader of the documentation. Assigning `vaadin-bot` is the manual
# override: it runs the bot on a pull request the type filter passed over.
if: >
  github.event.pull_request.draft == false && (
    (github.event.action == 'assigned' &&
     github.event.assignee.login == 'vaadin-bot') ||
    (github.event.action != 'assigned' &&
     !startsWith(github.event.pull_request.title, 'test:') &&
     !startsWith(github.event.pull_request.title, 'test(') &&
     !startsWith(github.event.pull_request.title, 'ci:') &&
     !startsWith(github.event.pull_request.title, 'ci(') &&
     !startsWith(github.event.pull_request.title, 'refactor:') &&
     !startsWith(github.event.pull_request.title, 'refactor(') &&
     !startsWith(github.event.pull_request.title, 'chore:') &&
     !startsWith(github.event.pull_request.title, 'chore(') &&
     !startsWith(github.event.pull_request.title, 'build:') &&
     !startsWith(github.event.pull_request.title, 'build('))
  )

permissions:
  contents: read
  pull-requests: read

engine: claude

tools:
  github:
    # gh-proxy routes GitHub API access through the pre-authenticated gh CLI,
    # so api.github.com does not need to be in the network allowlist. The
    # `search` toolset is gone: vaadin/docs is checked out below, so `grep`
    # answers the same questions for free.
    mode: gh-proxy
    toolsets: [repos, pull_requests]
    github-token: ${{ secrets.VAADIN_BOT_TOKEN }}
  edit:
  bash: true

# Allow GitHub domains (github.com, *.githubusercontent.com, ...) so git
# operations against the vaadin/docs checkout can reach the remote.
network:
  allowed:
    - defaults
    - github

timeout-minutes: 30

# A burst of pushes to the same pull request collapses into one run of the
# latest state. Each run analyses the whole pull request, never just the
# increment, so a cancelled run loses nothing.
concurrency:
  group: doc-bot-${{ github.event.pull_request.number }}
  cancel-in-progress: true

# vaadin/docs is checked out beside this repository so the agent reads it with
# `grep` instead of the code-search API. `doc-bot/*` brings in the branch of an
# existing documentation pull request, which Phase 5b commits onto.
checkout:
  - fetch-depth: 1
  - repository: vaadin/docs
    path: docs-repo
    ref: main
    fetch: ['doc-bot/*']
    github-token: ${{ secrets.VAADIN_BOT_TOKEN }}

env:
  SOURCE_REPO: ${{ github.repository }}
  PR_NUMBER: ${{ github.event.pull_request.number }}
  PR_AUTHOR: ${{ github.event.pull_request.user.login }}
  PR_TITLE: ${{ github.event.pull_request.title }}
  PR_HEAD_SHA: ${{ github.event.pull_request.head.sha }}

safe-outputs:
  create-pull-request:
    target-repo: "vaadin/docs"
    base-branch: "main"
    title-prefix: "[docs] "
    labels: [documentation, automated]
    draft: true
    expires: 30
    if-no-changes: ignore
    fallback-as-issue: true
    github-token: ${{ secrets.VAADIN_BOT_TOKEN }}
  # Updates the documentation pull request opened by an earlier run instead of
  # opening a second one for the same source pull request. `target: "*"` lets
  # the agent name the pull request to push to, so the required title prefix
  # and labels narrow that to the ones this workflow itself opened.
  push-to-pull-request-branch:
    target: "*"
    target-repo: "vaadin/docs"
    required-title-prefix: "[docs] "
    required-labels: [documentation, automated]
    if-no-changes: ignore
    github-token: ${{ secrets.VAADIN_BOT_TOKEN }}
  assign-to-user:
    target: "*"
    target-repo: "vaadin/docs"
    github-token: ${{ secrets.VAADIN_BOT_TOKEN }}
  add-comment:
    # One standing comment per pull request: a re-run supersedes its
    # predecessor instead of stacking a note onto every push.
    hide-older-comments: true
  # Lets the agent record "nothing to document, because …" in the run log
  # without putting anything on the pull request.
  noop:
    report-as-issue: false
---

# Documentation Bot

You analyze a pull request in `${{ env.SOURCE_REPO }}` and, when it changes something a reader would need to know about, you keep a matching documentation pull request in `vaadin/docs` up to date with it.

You run on **every push** to every non-draft pull request that survives the trigger filters. Most of your runs therefore end in Phase 2 or Phase 4 with nothing to do, and that is the expected outcome, not a failure.

## Environment

- **PR:** `${{ env.SOURCE_REPO }}#${{ env.PR_NUMBER }}` — ${{ env.PR_TITLE }}
- **Author:** @${{ env.PR_AUTHOR }}
- **Head commit:** `${{ env.PR_HEAD_SHA }}`

Two repositories are checked out for you:

- The workspace root holds `${{ env.SOURCE_REPO }}` at the pull request's merge commit, so `cat` and `sed` give you the post-change state of any file.
- `docs-repo/` holds `vaadin/docs` at `main`, plus any `doc-bot/*` branch.

Never modify a file in the workspace root. All of your edits go into `docs-repo/`.

## Phase 0: Which documentation pull request is this?

Every documentation pull request for this source pull request lives on the branch `doc-bot/vaadin-flow/${{ env.PR_NUMBER }}`. Check whether it exists:

```bash
git -C docs-repo rev-parse --verify "origin/doc-bot/vaadin-flow/${PR_NUMBER}"
```

If it does, find the pull request in `vaadin/docs` whose head branch is that name. What you find decides how this run ends:

| State | What this run does |
|---|---|
| No branch, or a branch with no pull request | **Create** one (Phase 5a). |
| An **open** pull request | **Update** it — commit onto its branch (Phase 5b). |
| A **merged** pull request | Create a new one (Phase 5a) on branch `doc-bot/vaadin-flow/${{ env.PR_NUMBER }}-<first 7 characters of the head commit>`, covering only what changed since the merged one. |
| A **closed, unmerged** pull request | Someone rejected the documentation for this change. Record a `noop` saying so and stop. Do not reopen it and do not open another. |

## Phase 1: Analyze the Pull Request

Always analyze the **whole** pull request, never just the commits of the latest push. A run can be cancelled by a newer push, so the increment since the previous run is not a reliable unit of work — the full diff is.

1. **List the changed files first.** Fetch diffs only for the user-facing ones, at most 20, and skip any file with more than 500 lines changed — note those in the pull request body instead.
2. **Read the pull request metadata** — title, description, and top-level comments only.

Classify each meaningful change into one or more of these categories:

| Category | Description |
|---|---|
| `NEW_FEATURE` | A new user-facing feature, component, or API |
| `API_CHANGE` | Modification to an existing public API (signature, return type, parameters) |
| `BEHAVIOR_CHANGE` | Change in existing behavior visible to end-users or developers |
| `DEPRECATION` | A public API or feature is deprecated |
| `BREAKING_CHANGE` | A change that breaks backward compatibility |
| `INTERNAL_ONLY` | Refactoring, internal implementation changes with no user-facing impact |
| `TEST_ONLY` | Changes only to test files |
| `BUILD_ONLY` | Changes only to build configuration, CI, dependencies |

## Phase 2: Early Exit Check

If **all** changes are `INTERNAL_ONLY`, `TEST_ONLY`, or `BUILD_ONLY`:

- If no documentation pull request exists yet, record a `noop` naming the reason and stop. Do not comment on the source pull request — you run on every push, and a note saying nothing happened on each of them is noise.
- If one exists, the change that justified it may have been reverted. Continue to Phase 3; Phase 4 decides whether the documentation still matches the pull request.

## Phase 3: Plan the Documentation Changes

Flow changes almost always land in `docs-repo/articles/flow/`, component changes in `docs-repo/articles/components/`; `ls docs-repo/articles/` shows the rest (`hilla/`, `building-apps/`, `styling/`, `tools/`, `getting-started/`, `upgrading/`).

For each user-facing change from Phase 1:

1. **Find the pages that already mention it** — `grep -rn "ClassName" docs-repo/articles/flow/`. Search by class name, configuration property, or feature name; searching for every method name is rarely worth it.
2. **Pick the target files.** Prefer updating an existing page over creating a new one.
3. **Decide what has to be added, changed, or removed** in each.

Scope:

- **5-8 files maximum**, so the pull request stays reviewable. When the source pull request changes more than ~50 files, cover the most significant public-API and feature changes and leave the rest as a checklist in the pull request body.
- Mark anything you are unsure about with `// TODO: Verify this documentation change — auto-generated by doc-bot` and list it under a **Needs human review** heading in the pull request body. Never guess and never fabricate.

## Phase 4: Write the Documentation

Documentation is **AsciiDoc** (`.adoc`) with YAML front matter. Do not read files to learn the style — use these rules: code blocks are `[source,java]` with `----` delimiters, cross-references are `<<filename#anchor,text>>`, and admonitions are `[NOTE]`, `[TIP]`, `[WARNING]`. Preserve the heading levels, structure, and voice of any file you edit.

1. **Start from the right branch** inside `docs-repo/`. Both safe-outputs take the changes from a commit, so give the checkout a git identity first:

   ```bash
   git -C docs-repo config user.name "vaadin-bot"
   git -C docs-repo config user.email "vaadin-bot@users.noreply.github.com"
   git -C docs-repo switch -c "doc-bot/vaadin-flow/${PR_NUMBER}" <start point>
   ```

   The start point is `origin/main` when you are creating (Phase 5a) and `origin/doc-bot/vaadin-flow/${PR_NUMBER}` when you are updating (Phase 5b).

2. **Edit the files.**
   - Only document what the source pull request actually changes.
   - Never remove existing documentation without clear justification from the source pull request.
   - When updating, revise in place rather than appending a second description of the same API, and drop documentation an earlier run wrote for something the pull request no longer does. A dropped commit has to drop its documentation with it.

3. **Decide whether anything actually changed.** Run `git -C docs-repo status --porcelain`. If it is empty, the documentation already describes the current state of the pull request — the push you are reacting to changed nothing a reader would see. Record a `noop` saying so and stop. This is the common outcome on later pushes, and it is what keeps the bot quiet.

4. **Commit** on that branch:

   ```
   docs: update documentation for ${{ env.SOURCE_REPO }}#${{ env.PR_NUMBER }}

   Source PR: ${{ env.PR_TITLE }}
   Source commit: ${{ env.PR_HEAD_SHA }}
   Categories: <comma-separated list of change categories>
   ```

## Phase 5a: Create the Documentation PR

Use the `create-pull-request` safe-output with `repo` set to `vaadin/docs`.

**Title:** `Update docs for ${{ env.SOURCE_REPO }}#${{ env.PR_NUMBER }}: ${{ env.PR_TITLE }}` — the `[docs] ` prefix is added for you.

**Body:**

```markdown
Documentation for ${{ env.SOURCE_REPO }}#${{ env.PR_NUMBER }} by @${{ env.PR_AUTHOR }}.

> [!NOTE]
> The source pull request is still open. This one tracks it and is updated
> automatically whenever the source changes what a reader sees. Merge it once
> the source pull request is merged.

**Change categories:** <the categories from Phase 1>

| File | Change |
|------|--------|
| <file> | <what changed and why> |

Auto-generated by the Documentation Bot — review before merging. Anything
marked `TODO: Verify` needs a closer look.
```

Then assign the new pull request to `@${{ env.PR_AUTHOR }}` with the `assign-to-user` safe-output. If they cannot be assigned — no access to `vaadin/docs`, say — note it in the body and carry on; this is not a failure.

## Phase 5b: Update the Existing Documentation PR

Use the `push-to-pull-request-branch` safe-output with `repo` `vaadin/docs`, the `pull_request_number` from Phase 0, `branch` `doc-bot/vaadin-flow/${{ env.PR_NUMBER }}`, and the Phase 4 commit message.

Do not re-assign the pull request and do not rewrite its description; the reviewer already has both.

## Phase 6: Comment on the Source PR

Add one comment with the `add-comment` safe-output. A later run replaces it, so describe the current state rather than what this run did:

> **Documentation Bot:** Draft documentation pull request for this change: vaadin/docs#\<NUMBER\>
>
> Files updated:
> - `<file1>`
> - `<file2>`
>
> It is kept up to date automatically on each push here. Please review it and mark it ready for review once this pull request is ready to merge.

Do not comment when you recorded a `noop`.
