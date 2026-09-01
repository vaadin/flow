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

Build the grid: **every module in scope × every rule in `PORTING.md`**, one
verdict per cell — `pass`, `fail`, or `waived` (naming the waiver).

**Rows** are the modules this PR adds or changes, **plus every module elsewhere in
the ported tree that holds an obligation this PR discharges.** Concretely: for
each class first ported in this PR, search the ported tree for the rule-12
placeholder slices that stood in for it — their doc says "the slice of X that …"
or calls itself a port deviation. Every hit is a row, because rule 12 requires the
slice to disappear once the real class lands. *Decision procedure:* placeholder
contracts naming a class ported in this PR = 0, or one retrofit-backlog row each.

Derive each module's Java counterpart by reversing rule 2's path mapping
(`internal/<rest>/X.ts` ↔ `com.vaadin.<rest>.X`; `…/XTests.ts` ↔ `…XTest`), so
the pairing is mechanical rather than guessed.

For a **test** suite that mapping gives the file location, not the counterpart
set: enumerate the Java test classes per rule 13.9 — the JRE-side `XTest`, the
`GwtXTest` under `src/test-gwt`, a `JreXTest` where one exists, and the
`flow-server` test for a class ported from `com.vaadin.flow.*` — and compare
against the **union** of their `@Test` methods.

Compare it in **both directions**, and say so per direction. Checking that every
`// Ported from X` names a real `@Test` proves the citations are honest; it does
**not** prove the coverage is complete. Only the reverse — every `@Test` in the
union appearing in some citation — finds a case nobody ported and nobody
recorded. *Decision procedure:* the report gives two counts, citations that
resolve to a Java `@Test` and union cases that appear in a citation, and names
every case missing from the second.

> Regression this prevents: a review reported test parity as clean with all 56
> citations verified, while `GwtDependencyLoaderTest.testDependenciesWithAllLoadModesAreProcessed`
> had no `it()` and no backlog row — the forward check cannot see an absence.

Two things make the reverse count easy to get wrong, and both have already
produced a false clean:

- **A mention is not a citation.** Match the Java name only where it appears in a
  `// Ported from` line. A case whose name sits in a bare comment above the
  `it()` — or in the suite's prose — reads as covered while the case itself is
  labelled *Beyond the Java suite*.
- **Citations wrap.** A citation naming two Java cases, or one long name, spans
  two comment lines; matching only the first line reports the second name as
  uncited.

Filter to test methods as well: a JRE-side test class also declares its mock
collaborators' methods (`sendNodeSyncMessage`, `inlineStyleSheet`, …), and
counting those as cases invents uncited names. A method counts when it is
**`@Test`-annotated *or* named `test…`** — `src/test-gwt` mixes both conventions,
`GwtClientJsonCodecTest` annotating its cases while `GwtDependencyLoaderTest`,
`GwtExecuteJavaScriptElementUtilsTest` and `GwtStateTreeTest` rely on the JUnit 3
naming their `GWTTestCase` base needs. Requiring the annotation alone drops those
classes to zero cases, which reads as a clean reverse count over the part of the
suite with the most coverage.

> Regression this prevents: two `StateTreeTests` cases were labelled *Beyond the
> Java suite* directly above a bare comment naming their `GwtStateTreeTest`
> counterpart, and a reverse check that accepted any mention read them as covered.

*Decision procedure:* the report lists the counterparts found per suite and the
union's case count, so a suite that claims to have none has demonstrably looked.

**Report evidence, not verdicts.** The grid may be summarised **per rule** — never
per module — and each rule row carries what established it: the counts compared,
or the members listed. *"Rule 6: `@param`/`@returns` per module 5/2, 4/2, 16/12/3,
30/11, 4/2, 6/4 — each equal to its Java original"* is a row. *"Every other cell
`pass`"* is not; it counts as an empty cell, and a missing cell is an incomplete
review rather than an implicit pass. A summary verdict such as "the port is
faithful" without the grid is not a review at all.

> Regressions this prevents: `@param`/`@returns` were missing from all seven
> state-tree modules while the review reported the production code as faithful —
> the modules that happened to be spot-checked were the ones that were fine. Later,
> `MapProperty`'s rule-12 slices survived a review that marked every module `pass`,
> because they live in the PR below and were outside the rows.

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

From `flow-client/`:

| Command | Runs in a clean checkout? | Covers |
| --- | --- | --- |
| `npm ci` | yes | install — run it first |
| `npm run lint` | yes | eslint, including `tsdoc/syntax` and `no-console`. **Not** a type-check |
| `npm test` | yes | the ported suites. `FlowTests.ts` **fails without a Maven build** — it imports the GWT-built `FlowClient.js`. Expected noise, not a regression |
| `npm run typecheck-tests` | yes | `tsc` over `src/test/frontend/tsconfig.json`: the ported suites and every module they import. A **real type-check**, and the only one that needs no Maven build |
| `npm run compile`, `npm run build` | **no** | both run `scripts/client.js`, which reads the GWT output under `target/classes/META-INF/resources/VAADIN/static/client/` and fails with `ENOENT` until Maven has built it |
| `mvn -pl flow-client install` | yes, slow | everything: GWT compile, then `tsc` over `src/main/frontend`, then the test-tree check, then lint |

So lint, the suites and the test-tree type-check are all cheap and always
available. What still costs a full `mvn -pl flow-client install` is the whole-tree
check of `src/main/frontend`: its `tsc` emits, and `npm run build` puts
`scripts/client.js` in front of it. A module no suite imports is therefore only
covered by that build, so state which of the two you ran. If you ran neither, write
that the type-check is **unverified** and leave it to CI — "lint passed" is not
"types check".

Paste the real output of what you ran. If a command could not run, write that the
work is **unverified** and why. There is no third state, and "should be clean" is
not a verification.

## 5. Fixing in the same run

A single invocation carries the whole cycle: enumerate → fix → re-verify →
report.

- After fixing, re-derive each affected verdict **from the files**, not from your
  own earlier report. A run that fixes something does not get to certify it from
  memory.
- **A finding that is a class, not an instance, is swept in the same run.** When a
  deviation could exist elsewhere — or you add a rule — inventory the whole ported
  tree for it before reporting, and include the inventory: site, Java original,
  verdict. Fix per §8.3; anything deferred gets a retrofit-backlog row. (Regression
  this prevents: the `?.` → `!` sweep of the base layer happened only because a
  maintainer asked for it after the PR-local fix.)
- One rule-class per commit, so each commit has a single decision procedure and
  can be reviewed or reverted on its own.
- Do not rebase or force-push unless the request says to. An earlier round
  silently lost a requested `@since` change that way, and nobody noticed for two
  rounds.
- **A run that pushes commits updates the PR description in the same run.** The
  description is a reviewed artifact (§6), but a review only checks it as of that
  pass — so the run that changes the code owns the update: the test count when
  suites changed, and the §8.4 declaration when a commit touched a file owned by a
  lower PR. Before finishing, compare the PR's changed-file list (§0) against what
  the description claims; if they disagree, the description is wrong, not the list.
  (Regression this prevents: three retrofit commits landed after a review, leaving
  the §8.4 list naming two base-layer files while five more had been modified, and
  the "otherwise only this PR's own new files" sentence false — one commit after
  the rule requiring that declaration was written.)

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

**The PR description is one of the reviewed artifacts.** Its test count must match
the suites, and any change to files owned by a lower PR must be declared per §8.4.
A mismatch is a finding like any other, reported with the corrected numbers. §5
puts the update itself on the run that pushes commits, so a review normally only
confirms the description rather than repairing it.
(Regression this prevents: a description claimed "42 tests" against 57 and "no
edits to existing code" while two base-layer files carried retrofits, through three
review rounds.)

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

## 9. Look for deferred improvements, and file them

Every review ends with one pass that asks a different question: not "is this
faithful to Java?" but "is faithful also the best TypeScript can do?" Sweep the
modules in scope for these shapes — each has already produced a real entry:

- a type parameter with **no inference site** (it appears only in the return type),
  so the caller declares it and nothing checks the claim;
- an `as` cast, a `!` or a `@ts-expect-error` that exists **only** to satisfy a
  Java shape;
- a **boolean predicate used for narrowing**, where a type guard would infer the
  result;
- an **overload set** that a single generic signature would collapse;
- a **structural check standing in for `instanceof`**, or `unknown` / `any` where
  Java had a real type parameter;
- `number` or `string` parameters that a **literal union** of the ported constants
  would constrain;
- a module or member that exists **only because the Java file exists**, whose body
  is a one-liner over a native TypeScript construct.

File each hit in [`PORTING-IMPROVEMENTS.md`](./PORTING-IMPROVEMENTS.md) per rule
16 — the site stays `pass` in the grid, because the port is faithful — and list
what you filed in the report. A review that files nothing says so explicitly, so
"nothing found" is a claim rather than a silence.
