<!--
 Copyright 2000-2026 Vaadin Ltd.

 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy of
 the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations under
 the License.
-->

# Reviewing a port against PORTING.md

The procedure for checking a migration PR against
[`PORTING.md`](./PORTING.md). It exists because the same findings kept recurring
across #24933, #24947 and #24948 — not because the rules were unclear, but
because each round re-derived them by hand, sampled instead of enumerating, and
reported verdicts without evidence. Every section below closes one of those
holes.

Follow it in order. A review that skips a section is not a review of
`PORTING.md`.

## 0. Establish what you are looking at

The review workflow checks out the repository's **default branch** at depth 1, so
the working tree is probably *not* the pull request and there is no base history.

Before reviewing, state in the report: the PR number, the head SHA you actually
read, and how you obtained the files (working tree, or the PR's changed-file list
read through the GitHub tools). If you cannot obtain the PR's files, say so and
stop. Do not review the default branch and call it a review of the PR.

## 1. Enumerate; never sample

Build the grid: **every ported module in this PR × every rule in `PORTING.md`**,
one verdict per cell — `pass`, `fail`, or `waived` (naming the waiver).

Derive each module's Java counterpart by reversing rule 2's path mapping
(`internal/<rest>/X.ts` ↔ `com.vaadin.<rest>.X`; `…/XTests.ts` ↔ `…XTest`), so
the pairing is mechanical rather than guessed.

A missing cell is an incomplete review, not an implicit pass. A summary verdict
such as "the port is faithful" without the grid is not a review.

> Regression this prevents: `@param`/`@returns` were missing from all seven
> state-tree modules while the review reported the production code as faithful —
> the modules that happened to be spot-checked were the ones that were fine.

## 2. Cite both sides, or it is not a finding

Every reported item carries three things: the TypeScript `file:line`, the Java
`file:line` it deviates from, and the rule number it violates.

A clean verdict needs evidence too: "rule 3 holds" means naming the exported
symbols you compared against the Java modifiers.

> Regression this prevents: a review reported "visibility parity checks out"
> while `createReturnChannelCallback` and `applyCaptures` — `private static
> native` at `ClientJsonCodec.java:161` and `:224` — were exported.

## 3. Give every finding a decision procedure

State the check that settles the finding, as a count or comparison over named
files — for example *"`@see` occurrences in `ClientJsonCodec.ts` = 1"*, or
*"`it()` count = Java `@Test` count − cases marked "Beyond the Java suite" −
cases documented as deferred"*.

Phrase it as a file-and-count statement, not a shell command: this environment
grants no `git` or `grep` shell access, and the next run has to be able to
re-check it identically.

If only human judgement can settle an item, label it `judgement` and state the
trade-off instead of inventing a threshold.

## 4. Verify with the commands that exist

Runnable here, from `flow-client/`:

| Command | Covers |
| --- | --- |
| `npm ci` | install (required first in a clean checkout) |
| `npm run lint` | eslint, including `tsdoc/syntax` and `no-console` |
| `npm run compile` | builds, type-checks with `tsc`, then lints |
| `npm test` | web-test-runner suites |

`npx tsc --noEmit` is **not** available — use `npm run compile` for the
type-check.

Paste the real output of what you ran. If a command could not run, write that the
work is **unverified** and why. There is no third state, and "should be clean" is
not a verification.

## 5. Fixing in the same run

A single invocation carries the whole cycle: enumerate → fix → re-verify →
report.

- After fixing, re-derive each affected verdict **from the files**, not from your
  own earlier report. A run that fixes something does not get to certify it from
  memory.
- One rule-class per commit, so each commit has a single decision procedure and
  can be reviewed or reverted on its own.
- Do not rebase or force-push unless the request says to. An earlier round
  silently lost a requested `@since` change that way, and nobody noticed for two
  rounds.

## 6. Report shape

One comment containing, in order:

1. the §0 provenance line (PR, head SHA, how the files were read);
2. the §1 grid;
3. a findings table — rule, TypeScript ref, Java ref, decision procedure;
4. the §4 verification output;
5. a final **"Not verified"** list.

Open one inline review thread per `judgement` finding, so it carries its own
resolvable state. Mechanical findings stay in the grid, where the next run
re-derives them.

## 7. Do not reopen settled decisions

`PORTING.md` rules 7, 11 and 14 record decisions already taken for the whole
series — `@since`/`@author` not carried, deferral notes for unported
dependencies, and the language mappings including strict `===`. Cite the rule
number; do not re-argue it. New evidence is the only reason to reopen one, and it
goes in its own thread.

## 8. When the review discovers a new rule class

A rule that did not exist when an earlier branch was reviewed is **not a
violation of that branch**. Do not reopen approved PRs, and do not rebase the
stack to retrofit it.

1. **Write the rule down first** — in `PORTING.md`, with the branch it was
   introduced at (`_Introduced during #NNNNN._`). Everything below depends on that
   marker existing.
2. **Classify it.**
   - **Surface** — module and path layout, export visibility, public API shape,
     the set of ported test cases. Leaving these inconsistent makes the
     convention undecidable for later ports, so they must be retrofitted.
   - **Cosmetic** — doc tags, wording, member order, test-case order, markers.
     Correctness is unaffected; the retrofit is a convenience and can be
     scheduled.
3. **Fix forward in the stack, never backwards.** A file introduced in an earlier
   PR also exists in the tree of every branch above it, so a retrofit landing in
   a higher branch still leaves the default branch correct once both merge — with
   no rebase of approved work. Choose by the earlier PR's state:

   | Earlier PR | Where the retrofit goes |
   | --- | --- |
   | open, not yet approved | in that PR — cheapest, no fan-out |
   | approved / awaiting merge | the lowest open branch above it, as its own commit |
   | already merged | a separate follow-up PR against the default branch |

4. **Say so in the PR that carries it.** A retrofit commit touches files that
   belong to a lower PR, which otherwise reads as scope creep or as a bad rebase.
   One line in the description is enough: *"also carries the rule-14.4 retrofit
   for files introduced in #24947."*
5. **Record anything not fixed in the same run** in the retrofit backlog in
   `PORTING.md` — rule, affected modules, target branch, status. Cosmetic
   retrofits may be batched into a single later cleanup, but only if they are on
   that list.
6. **Grade older code against the rules that existed for it.** In the §1 grid, a
   cell whose rule postdates the branch under review is
   `n/a (rule introduced in #NNNNN)`, not `fail` — unless the retrofit has already
   landed, in which case grade it normally. Without this, every new rule turns
   every earlier branch red and the grid stops carrying information.

> Regression this prevents: rule 2's path mapping arrived mid-series and forced a
> 31-module relayout across two already-reviewed branches, while `@since` was
> requested, applied and then dropped again — both because there was no way to
> say "new rule, applies from here".
