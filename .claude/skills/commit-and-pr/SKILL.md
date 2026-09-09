---
name: commit-and-pr
description: How to write a commit message and a pull request description in this repository - the commit subject and body rules, and a PR body a reviewer grasps in under a minute (typed header, plain-words summary, explicit risk list, what-changed bullets, how to test, collapsed detail). Use before writing a commit message, before opening a pull request, and when rewriting an existing PR body.
---

# Commits and pull requests

These are the canonical rules for this repository; the Commit & PR Hygiene
section of [`CONVENTIONS.md`](../../../CONVENTIONS.md) points here.

Run `mvn spotless:apply` before committing. Write commit messages terse and
exact, no fluff.

## Commit messages

### Subject line

- Format: `<type>: <summary>`, or `<type>(<scope>): <summary>` (Conventional Commits)
- Length: under 72 chars
- Allowed types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`
- Breaking changes: append `!` before the colon (`feat!`, `fix!`, `chore!`)
- Start with an imperative verb — "add", "fix", "remove" — not "added", "adds", "adding"
- Use English articles only when omitting them would be ambiguous
  - Good: `fix: handle null in parser`
  - Avoid: `fix: handle null in the parser`
- Don't repeat the type in the summary: avoid `fix: fix...`, `refactor: refactor...`
- Add the affected module or area as a scope when it narrows the message
  usefully — `fix(flow-client):`, `feat(hilla):`, `chore(deps):`. The
  scope is optional; a bare `fix:` is fine for changes that span modules.

### Body

- Skip when the subject is self-explanatory
- Add a body only for: non-obvious *why*, breaking changes, migration notes
- Add `Fixes #issuenumber` when the commit resolves an issue in this repository

## Pull request title

Use the same rules as for commit subject lines.

## Pull request description

The goal is a description a reviewer can read in under a minute: issue links, a
lede they can understand without opening the diff, the risks spelled out, a
bullet list of what changed, a type label, and concrete steps to verify by hand.

Two rules shape it.

- **The first screen is a budget, not a summary of the body.** About 25 visible
  lines. Long tables, API diffs and per-test rationale go inside `<details>` —
  nothing here asks for less detail, only for it to be out of the way.
- **Anything that can surprise a reviewer is promoted to the top**, even when it
  is one line buried in a 60-line implementation section.

### The template

Fixed section order. Omit a section instead of leaving it empty.

````markdown
## Description

Fixes https://github.com/vaadin/flow/issues/951

**<Problem type>** · <module> · <who is affected>

**Background — <concept>.** <what the concept is, at most 3 sentences>

<Summary: 1-3 sentences, plain words>

**Risks:**
- <only the flags that are not empty>
- ✅ <one line for everything that is clean>

**Context.** <why our code was like this, at most 3 sentences>

- <What changed, one behavior per bullet>
- <…>
  - <Sub-bullet: a detail or the reason, only when the parent bullet needs it>

## Type of change

- <Feature | Bugfix | Refactor | Documentation | Tests | Internal change>

## How to test

1. <Open an integration test view: `flow-tests/test-root-context/…/ExecJavaScriptView.java`>
2. <Do the thing>
3. <What you should see>

<details><summary><b>API changes</b></summary>

…
</details>
````

Nothing above `## Description`, nothing below the last section — no `## Checklist`
block, no footer, no AI attribution. GitHub inserts the organisation template
into a new PR; delete the parts this template does not use.

### Issue links

- Links first, one per line, no bullet. Omit the block when there is nothing to link.
- Full URLs for cross-repo links, bare `#NNNN` only within the same repo.
- Pick the most specific relation; combine when both genuinely fit: `Fixes #123, Part of #456`.

| Relation         | When to use                                                                                                         |
| ---------------- | ------------------------------------------------------------------------------------------------------------------- |
| `Fixes #`        | Resolves a bug. Don't use for features — they usually span multiple PRs or repos and are closed manually.           |
| `Part of #`      | One slice of a feature/task that spans multiple PRs or repos. Always add when there's a platform ticket with a PRD. |
| `Depends on #`   | Must be merged after another PR.                                                                                    |
| `Follow-up to #` | Addresses something missed or deferred by a previously merged PR.                                                   |
| `Related to #`   | Touches the same area as another issue/PR without fixing or implementing it.                                        |

### Header line

`**<Problem type>** · <module> · <who is affected>`

One line, so the nature and the severity of the change are readable before
anything else is read. The problem type comes from this closed list:

`memory leak` · `serialization` · `behavior deviation` · `regression` ·
`build failure` · `missing API` · `security` · `performance` · `DX/docs` ·
`new feature` · `refactor`

"Who is affected" is a condition, not a module: *apps that set custom `@PWA`
paths*, *production builds on a clean checkout*, *everyone using `Grid` with a
lazy data provider*.

This is not the same thing as `## Type of change` below: the header word says
what kind of trouble the change is about, the section says what kind of work it
is. For a `new feature` or a `refactor` the two do overlap; keep both anyway.

### Background — optional

What the concept **is**, independent of this PR. True before the change and
after it.

Include it when the PR leans on something a reviewer from a neighbouring area
may not know: JSR-303 validation groups, service worker precache, the hotswap
agent, state tree resync, JWT stateless authentication, `@GroupSequence`. Skip
it for everyday Flow — `Grid`, `@Route`, `Binder` as such.

- At most 3 sentences, plus optionally one snippet of at most 8 lines.
- The snippet shows **the concept, not the change**.
- Written for somebody who knows Java and Flow but not this corner of it.
- Not one word about what the PR does. That is the next paragraph.

### Summary

1. At most 3 sentences, at most 60 words. Hard limit.
2. **No class, method or file names.** They belong in the bullets and the
   details. The exception is what the user sees: `@PWA`, `Grid`, "production build".
3. Symptom before mechanism — "the build fails on a clean checkout", not
   "`BundleValidationUtil` did not check the bundle".
4. Say who hits it and whether there is a workaround.
5. The test: a colleague who has never opened these files can retell the PR in
   their own words.

A short usage snippet may follow the summary when the PR adds API — at most 8
lines, showing the everyday call, not the full scenario.

### Risks

Walk the whole checklist every time. Print only the entries that are **not**
empty, then close with a single line for everything that is clean. A wall of
"no / no / no" is exactly the noise this format exists to remove; an empty list
would hide that the author checked at all.

| Flag | Icon | Look for |
|---|---|---|
| Breaking change | ⚠️ | a signature, default or contract that existing code depends on |
| Behaviour change | ⚠️ | same API, different outcome — including "it was wrong before" |
| Public API | ⚠️ | additions, `protected` members that subclasses override, deprecations |
| Security | 🔒 | authentication, authorization, permit lists, path matching, input trusted from the client |
| Memory & leaks | 🧠 | anything held per UI, per session or per binder; who releases it |
| Serialization | 💾 | `Serializable` fields, lambdas kept in fields, state tree and Jackson payloads |
| Threading & push | 🧵 | access outside the session lock, background threads, `UI.access` |
| Performance | ⏱️ | extra work per request, per render or per build |
| Migration needed | 🔀 | what an application has to change |
| Open question | ❓ | a decision the reviewer has to make, not a detail |

Each non-empty flag is one line: what it is, who it hits, and — for a behaviour
change — why the old behaviour was wrong.

### Context — optional

Why **our** code was like this, and which constraint the change has to live
with. Repository-specific, unlike Background. At most 3 sentences.

This is the block that removes half the review questions, because it explains
why an odd-looking change is the right one: *"reusing a pre-built bundle skips
`npm install`, so `node_modules` never exists — but the template parser still
looked for the file there."*

### What changed — bullets

- Past tense, one behavior per bullet, identifiers in backticks.
  - Good: `Added setValidationGroups(Class<?>...) to BeanValidationBinder, making the validated groups configurable`
  - Avoid: `Updated BeanValidationBinder.java` — a bullet per file restates the diff.
- Group related edits into one bullet: ten renamed call sites are one bullet.
- Sub-bullets carry detail or reason, not more changes.
- Tests and integration test views get their own bullets when they are part of the deliverable.
- Cap at ~10 bullets. Past that, either the bullets are too granular or the PR should be split.
- No vague verbs (`Improved`, `Enhanced`, `Various fixes`) and no hedging (`Should now work`).

### Plain English

Most Vaadin readers are non-native English speakers, so avoid advanced or
uncommon vocabulary.

- Avoid: "gated", "predicated", "obviate", "subsume", "short-circuit", "surface" (as a verb or noun for "expose" / "API").
- Prefer: "only runs when", "based on", "remove the need for", "include", "skip" or "exit early", "API" or "expose".
- Banned as padding: comprehensive, robust, properly, carefully, seamlessly, enhanced, leverage.
- Technical terms (API names, identifiers, library names) stay exact. Only the surrounding prose needs to be plain.

### Type of change

One plain bullet, not a checkbox. Map from the PR title prefix: `feat` → Feature,
`fix` → Bugfix, `refactor` → Refactor, `docs` → Documentation, `test` → Tests,
`chore`/`ci` → Internal change. Mixed branches take the type a reviewer cares
about most — a `fix` with supporting test cleanup is still a Bugfix.

### How to test

- Numbered steps a reviewer can follow without reading the diff. Each step is one action.
- Step 1 names an integration test view that exists in the repository
  (`flow-tests/*/src/main/java/**/<Name>View.java`) — verify the file is there.
- The last step states what should happen — a run with no observable result is not a test.
- Keyboard keys as `<kbd>Enter</kbd>`.
- A preamble line for a prerequisite: `On a touch device, or with touch emulation:`.
- Omit the whole section when the change cannot be exercised by hand — dependency
  bumps, internal refactors, build plugin changes. A missing section is better
  than "run the tests".

### Collapsed details

Use plain `<details>`, never `<details open>`: the point is that a long body does
not scare the reader away. Put a blank line after the `<summary>` line, or GitHub
will not render the markdown inside.

```markdown
<details><summary><b>API changes</b></summary>

- …
</details>
```

The usual sections, in this order, all optional: **What changed in detail**,
**Full use case**, **API changes**, **Test coverage**.

### Optional extras

Only when the change genuinely needs them, always after `How to test`:

- A behavior table — `| Case | Before | After |` — when the change alters several
  distinct cases and a list would not make the pattern clear.
- `> [!NOTE]` — a single callout for a side effect a reviewer should know about
  but that is not the point of the PR.
- `> [!WARNING]` — for breaking changes, explaining what breaks and why.

## Before posting

- Summary at most 3 sentences, no class or method names in it.
- Every risk flag walked; non-empty ones at the top, the rest in one ✅ line.
- Every behaviour change is in **Risks**, not only in the bullets.
- All `<details>` collapsed, blank line after each `<summary>`.
- Issue link present when there is an issue, with the right relation.
- No `## Checklist`, no footer, no AI attribution.
- Open the pull request as a draft, and self-review before marking it ready.
