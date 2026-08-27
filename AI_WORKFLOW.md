# AI-First Workflow for the Flow Team

**Status:** draft for team discussion.

Developers spend their time on **problems, design and decisions**. AI does the
**research, drafting, implementation, tests and revisions**.

Two layers: how a **project** runs (a PRD becomes a shipped increment, §3) and
how a **topic** moves (a problem becomes a merged PR, §4–5). Companion to
[CLAUDE.md](CLAUDE.md) and [DESIGN_GUIDELINES.md](DESIGN_GUIDELINES.md), which
hold the content; this holds the process.

---

## 1. Why

Four things eat most of our week:

1. **Research and design from a one-sentence ask** — days, sometimes months, of
   locating where it lands in Flow and reconstructing how that area works today,
   before any decision is possible.
2. **Reading the diff**, line by line, largely for mechanical correctness.
3. **Reviewing routine, high-volume changes** — bumps, refactors, migrations.
4. **Impact analysis, repeated per issue** — is this needed, what can it break.

Only a thin slice of that is judgement; the rest is legwork before it and volume
hiding it.

| Where the time goes | Judgement — **team** | Legwork — **AI** |
| --- | --- | --- |
| Design from a vague ask | Which design; what belongs in Flow | The area, prior art, drafting options |
| Reading the diff | Semantics, API, risk | Mechanical correctness — compiler, tests, linters |
| Routine bulk changes | Deciding a change *is* routine | Producing it, proving it was |
| "Needed? Breaks anything?" | The decision | Context and blast radius |

**AI does the legwork before the decision and the work after it. The team makes
the decision.** Cost of mistakes follows the same line: an implementation is
cheap to redo, a public API is not.

---

## 2. Roles

A role is two things: **what you owe, and what others may demand of you.** We
usually write down only the first, which is why people know their tasks but not
what to expect from each other.

| Role | Owes | Others may demand |
| --- | --- | --- |
| **PM** — stakeholder, owns the PRD, never 100% on one project | Who the user is, what problem, why now, what success looks like; written answers within a day | Clarity about the *problem*, and a straight answer on whether something still serves it |
| **The team** — everyone else, 100% allocated | The shipped increment: design, code, tests, docs, demo, DX, quality, usability. Deciding *how*, and *how little* | That it decides and ships without being chased, and asks when the PRD is ambiguous |
| **Lead** — one per piece of work, on the team; *not* its implementer. Called the **project lead** on a project and the **topic driver** on a single feature — same role, different scope | That the work reaches decisions and goes in the right direction at the right pace: a truthful board, nothing left unowned, blocked people unblocked | Discussion opened early, specific questions, a straight answer on what matters most right now, gaps named early rather than discovered late |
| **Consulted expert** — not on the team | Answers when asked | Nothing else — no deliverables, no attendance |

Where expectations quietly diverge today:

- **The PM is not QA.** Whatever the team ships is what users get. The PM may
  say *"this no longer solves the user's problem"* — that is PRD ownership. Not
  *"the quality bar is not met"* — that is ours.
- **The PM does not push work.** No assigning, no chasing, no status-collecting.
  Clarification is a **pull**: the team asks, the PM answers. If something needs
  chasing, we are missing a decision, not a chaser.
- **The lead is not the PM, and not a manager of people.** The PM watches that
  the business requirement is being fulfilled; the lead watches that the work is
  going in the right direction and on time. Neither of them decides the design —
  the round does.
- **The team connects the PRD to the work.** Nobody else spans that gap.
- **Nobody is "partly" on the team.** Partial membership is worse than absence:
  it blurs who owes what and leaves work half-done. If you cannot be 100%, you
  are a consulted expert — a real role with clear expectations.

**The lead** takes a piece of work and carries it to the end — a whole project,
or a single topic from problem to merge. It is one role at two scales: on a
project we call it the project lead, on a feature the topic driver, and on a
small feature the two are the same person doing the same thing.

The lead does not build it — AI does — and is accountable for the outcome rather
than for having typed it. Nor is the lead licensed to decide alone: the design is
decided in the round. A lead often is not the person with the most scar tissue in
that area, and that is the point — it forces context out of one head.

What the role is, concretely:

- **Opens the discussion early**, at revision 1 — a polished revision 4 reads as
  a fait accompli and gets worse input.
- **Asks sharp questions instead of open ones.** "Should detach cancel the
  pending update or queue it?" gets a decision where "thoughts?" gets silence.
- **Pulls in people by name**, and records every conclusion in the issue.
- **Watches direction and pace**, not just progress: are we building the right
  thing in the right order, is anything unowned, has someone been quietly stuck
  for two days, will this land on time — and says so early rather than at the end.
- **Keeps the board honest** and runs the round.
- **Decides alone only in the gaps between rounds**, marking it as unilateral.
  Reopen such a decision with an argument, not with a delay.

Leads rotate. On a project the lead is fixed for its duration.

---

## 3. Running a project

**A product is what the user can do, not what we implemented.** So the unit of
planning is a **use case**; *done* means a user can do it — API that reads well
from outside, docs, sane errors, a demo — not "the PRs are merged"; progress is
reported as capabilities gained; and quality and usability are ours, because
they are the product.

**Kickoff (day 0).** The PM presents the PRD. The team asks anything. **No
estimates, no scope commitment** — nobody understands the problem yet. Output:
the roster (100% each) and the first written questions to the PM.

**Days 0–2.** AI produces an Analysis Brief (§5) per candidate use case: where
it lands, how that area works today, what supporting it would cost.

**Day 2 — the scope meeting.** The team agrees the **Use Case List**: what we
will support, and what we identified and deliberately will **not**, one line of
why each. The second list carries as much weight as the first — it makes
"minimal" checkable and stops us re-litigating scope on day nine. The PM answers
questions and says whether the scope still fulfils the PRD; the team decides what
it builds.

**Minimal by default.** Build the least that fulfils the PRD. Ideas that emerge
mid-project go to the **parking lot** — one line, who raised it, why it looked
good — and **nobody starts on a parked idea**. At the end it is triaged through
normal intake. Good ideas are not the problem; good ideas started quietly are.

**Rituals.** The project has its own round (§4); members **skip their home
team's ceremonies** while on it — two rhythms is what makes 100% impossible. The
PM sees a working walkthrough weekly, to confirm we are solving the right
problem, not to accept or reject the work.

**The board.** Every task has a ticket, and the board is public and truthful: at
any moment anyone can see what is in progress, what is done, and what nobody has
picked up. Knowing what to do next should never require asking someone.

**Done does the reminding.** Demos, docs and DX tests get dropped because they
are tracked apart from the code and postponed one day at a time. So they belong
to the use case's definition of done — a use case with merged code and no docs is
*not done*, and the board shows it as not done. Nobody has to chase work that
cannot be marked complete without it.

**The lead does what structure cannot** (§2). A definition of done stops work
being forgotten; it cannot tell whether we are building the right thing in the
right order. What the lead does *not* do is remind people to write documentation
— that is the definition of done's job, and if it needed reminding we would not
really consider it part of the work.

---

## 4. Topics and the round

**The topic is the unit.** An issue, a forum thread and a PR are three views of
one problem. A topic has **one home issue** with everything linked to it;
several reports with one cause are one topic, discussed once. It is named as
**the problem in the user's words** — *"Grid loses selection after a refresh"*,
not *"add a keepSelection flag"*. It stays **one continuous discussion, from the
problem to the merge decision**; we do not drop the thread and restart it in a
PR with a different audience and half the context.

**The Topic Round.** The whole team walks the live topics together, daily, 30
minutes, run by the lead; a topic that needs no decision takes ten seconds.
**The round is where discussion happens; between rounds AI does the work.**
During a project the round is the daily and the topics are the use cases — one
ritual, not two.

| Topic state | The question the round answers |
| --- | --- |
| `new` | What is the problem, and is it ours? (not: what do we build) |
| `understood` | What is the design — and how do we build it? |
| `agreed` | Nothing. AI is building it; speak only if it is blocked |
| `in review` | Do we merge? |

- **Problem before solution, always.** A topic may not be discussed as an
  implementation until the team has said out loud what the problem is. Most
  disagreements about *how* are unnoticed disagreements about *what*.
- **Design and implementation in one pass.** The same conversation settles the
  design *and* the approach, so AI goes straight to a PR. Splitting across two
  rounds is the exception, for genuinely new ground.
- **Progress is never reported aloud** — it lives in the issue. That is what
  keeps this from becoming a status meeting.

Target: **agreed in the first round that sees it, merged in the next.**

| Step | Target | Limit |
| --- | --- | --- |
| Intake → Analysis Brief | 15 min | 2 h |
| Brief → in a round | next round | 1 day |
| Problem agreed → design agreed | same round | 2 rounds |
| Design agreed → draft PR | 1–4 h | 1 day |
| Draft PR → merge decision | next round | 2 rounds |
| **Intake → merge** | **2 rounds** | **4 rounds** |

Counting in rounds is deliberate: "this topic has taken four rounds" is harder
to ignore than "it has been a few days".

---

## 5. The pipeline

```mermaid
flowchart TD
    IN["Slack thread · GitHub issue · Vaadin Forum"]
    S0["Stage 0 · INTAKE<br/>everything becomes a GitHub issue"]
    S1["Stage 1 · ANALYSIS BRIEF<br/>AI, ~15 min<br/>context · verdict · sketch"]
    S2{"Stage 2 · TRIAGE<br/>round: what is the problem?"}
    RJ["close — the brief is the answer"]
    NI["question back to the reporter"]
    PK["shelved — the brief stays as the record"]
    S3["Stage 3 · DESIGN + APPROACH<br/>round: what, and how?<br/>AI drafts · team agrees · design:frozen"]
    S4["Stage 4 · IMPLEMENTATION<br/>AI · tests first · green CI · draft PR"]
    S5{"Stage 5 · REVIEW<br/>round: do we merge?"}
    S6["Stage 6 · MERGE<br/>a human owns it"]
    S7["Stage 7 · HARVEST<br/>rules → CLAUDE.md · DESIGN_GUIDELINES.md"]

    IN --> S0 --> S1 --> S2
    S2 -- rejected --> RJ
    S2 -- needs info --> NI
    S2 -- parked --> PK
    S2 -- accepted --> S3 --> S4 --> S5
    S5 -- comments --> S4
    S5 -- design wrong --> S3
    S5 -- approved --> S6 --> S7

    classDef round stroke-width:3px;
    class S2,S3,S5 round;
```

The three thick-bordered stages are where the round decides something — the same
conversation, continued. Everything else happens between rounds. The same
pipeline in plain text, for editors that do not render Mermaid:

```text
          Slack thread   ·   GitHub issue   ·   Vaadin Forum
                                │
  Stage 0  INTAKE               │  everything becomes a GitHub issue
                                ▼
  Stage 1  ANALYSIS BRIEF ............................ AI, ~15 min
           1. context   2. verdict   3. sketch
                                │
                                ▼
  Stage 2  TRIAGE ...................... round: what is the problem?
              ├── rejected ...... close; the brief is the answer
              ├── needs info .... question back to the reporter
              ├── parked ........ shelved, the brief stays as the record
              └── accepted
                                ▼
  Stage 3  DESIGN + APPROACH ........... round: what, and how?
           AI drafts · team agrees · design:frozen
                                ▼
  Stage 4  IMPLEMENTATION ............................ AI
           tests first · green CI · draft PR
                                ▼
  Stage 5  REVIEW ...................... round: do we merge?
              ├── comments ...... back to Stage 4
              ├── design wrong .. back to Stage 3
              └── approved
                                ▼
  Stage 6  MERGE ..................................... a human owns it
                                ▼
  Stage 7  HARVEST ................................... rules → CLAUDE.md,
                                                              DESIGN_GUIDELINES.md
```

**0 · Intake.** Everything becomes a GitHub issue; Slack and the forum are where
people talk. **Filing is always triggered by a human** — deciding something
deserves the team's attention is the first judgement call. React with the intake
emoji, or `@claude file this`, on any Slack message; `#forum` already carries
every forum topic from Forum Bot, so one gesture covers all sources. AI then
reads the **whole** thread via `<topic-url>.json` (the real problem is usually in
the replies, not the first post Slack quoted), opens the issue, links it back and
runs Stage 1. One person per week is on **intake duty**; a weekly digest agent
flags *repeated* questions, which are missing APIs or docs gaps rather than five
support requests.

**1 · Analysis Brief** (AI, 15 min) — context, verdict, sketch, plus what it
could not verify. It is also the pre-read that makes a round possible.

**2 · Triage** — clear rejections the lead makes alone; everything else goes
to a round. Shallow on purpose: "worth our design time?", not "is this right?".

**3 · Design and approach** — the artefact the team argues about, covering both
the design and how we intend to build it. *"@claude, rework it: use an event
instead of a callback, and define what happens on detach."* Every conclusion
lands back in the note, with a one-line "what changed and why" per revision.

**4 · Implementation** (AI) — a draft PR against the agreed note: tests first
(if they expose a design problem, **go back to Stage 3** rather than bend the
tests), green CI, a description reviewable without the diff, and **every
deviation from the note declared**. An undeclared deviation is the worst failure
mode of this process.

**5 · Review** — read the description: does it solve the problem? Is the API
what we agreed, and what we want to live with? What happens at the edges — null,
detach, concurrency, serialization, back-compat? Are the tests aimed at
behaviour? What is the blast radius? Comment in the PR, `@claude` revises;
reviewers do not push fixes themselves, because asking keeps the rule
harvestable. Anything with design content is decided in the round by the people
who agreed the design; small and routine changes async. Bouncing back to Stage 3
is a success, not a failure.

**6 · Merge** — approving means *"I understand this and I am comfortable owning
it."* Never approve to unblock someone.

**7 · Harvest** — a recurring comment becomes a rule in `CLAUDE.md` /
`DESIGN_GUIDELINES.md`, a recurring analysis becomes a skill, a recurring check
becomes a CI check. This is what makes the next cycle shorter than this one.

---

## 6. Templates

```markdown
ANALYSIS BRIEF
## Context     Area · how it works today · why · related API and decisions
## Verdict     accept / reject / needs info / park — why · alternatives
               (userland, add-on, docs) · cost of accepting
## Sketch      proposed API (signatures, no bodies) · affected modules ·
               lifecycle & threading · open questions for the team
## Confidence  verified: <what I read or ran>   assumed: <what I did not>
```

```markdown
DESIGN NOTE — Revision N; "changed in this revision" in one line
## Problem                 user-facing, not the solution
## Goals / Non-goals
## Proposed design         API with signatures and contracts
## Behaviour               lifecycle, threading, null, errors, edge cases
## How we will build it    approach, main pieces, anything AI needs decided
## Alternatives rejected   option — why not. This section is the point.
## Compatibility · Testing strategy · Risks
```

```markdown
PR DESCRIPTION
## What · Why              user-facing paragraph; link to the topic
## Design                  link to the agreed note + 5–15 lines restating it
## How it is implemented   which class does what; the non-obvious decisions.
                           Enough to review without opening the diff.
## Deviations from the note   none / list, with reasons
## Testing                 covered, and deliberately not covered
## Risk & blast radius     what breaks if this is wrong; how we would notice
Fixes #<issue>
```

---

## 7. What replaces line-by-line review

The real gate is the design discussion; automated gates (build, tests, ITs,
`spotless`, `checkstyle`, API compatibility) are a precondition for review, not
its outcome. On top of that, **a human still reads the code** — mark the PR
`review:code-required` — when it touches:

- public API surface;
- security-sensitive paths: request handling, session, class-loading,
  deserialization, path resolution;
- the client-server protocol, `StateNode` / `StateTree`, anything crossing the
  wire;
- concurrency, `UI.access`, push;
- performance-critical paths, or any change justified by performance;
- anything AI flagged as uncertain, or that deviates from the note.

Plus **one random PR per week, read in full.** This is our calibration: it tells
us whether the descriptions we trust match the code. A mismatch is a process
incident — discuss it and fix the rule, do not quietly fix the PR.

---

## 8. Culture

- **Design is discussed, code is generated.** Arguing about code in a PR means
  we skipped a design discussion.
- **A PR must be reviewable without the diff.** If it is not, the description is
  the defect.
- **A human always decides.** AI never merges, never closes as "won't fix",
  never changes an API contract on its own authority.
- **Never approve what you do not understand**, and own the merge afterwards.
  "The AI wrote it and CI was green" explains nothing.
- **Correct the constitution, not just the output.** Fixing the same thing twice
  by hand means we forgot Stage 7.
- **Reward good rejections.** A topic closed in an hour with a clear explanation
  is a first-class outcome.
- **No silent local rewrites.** If you take a topic over and write it yourself,
  say why — that is our best data about where this process is weak.

**Silence is our failure mode.** Our discussions rarely fail through conflict;
they fail through silence, reliably when we open an area nobody has seen before.
That silence is not agreement and not an absence of ideas — it is people waiting
until their thought is good enough to say. It never becomes good enough. Since
everything here runs on discussion, this is the habit that decides whether the
rest works.

- **Half-formed thoughts are the deliverable** of a design discussion, not a
  by-product. "I might be wrong, but…", "this worries me and I can't say why
  yet", "I don't understand this part" — the last one is the most useful
  sentence available and is almost never true for only one person.
- **Nobody defends "their" design.** It belongs to the team the moment it is on
  the screen. That is what makes it safe to attack, and safe to be wrong.
- **Everyone speaks before anyone concludes.** Passing is allowed; staying
  invisible is not. A topic that passes through a round in silence was decided
  by whoever spoke last, not by the team.
- **Nobody arrives cold.** Silence usually means nobody has context, and
  reacting is far easier than originating — which is what the Analysis Brief is
  for. No brief, no design discussion.
- **Written threads are first-class**, not a fallback: the quietest person in a
  call often writes the sharpest thing in the thread.
- **A silent meeting is not diligence.** Ten minutes of only the lead talking
  ends the meeting; rebook it with a pre-read.

---

## 9. Adoption, signals, open questions

Adopt the **project layer (§3) whole** on the next project — roles, allocation
and rituals only work as a set. The **topic layer** can be phased: two weeks of
briefs only, then design notes, then the full pipeline with spot-checks, then
widen only while the spot-check mismatch rate stays low. Review this document at
the end of each phase.

Watch: rounds from problem to merge · share of topics agreed in the first round
that saw them · **spot-check mismatch rate** (the honesty metric) · bounces back
to design · rules added per month · use cases done vs. agreed on day 2 · how
much scope we managed *not* to build · how many people spoke.

Still to decide:

1. Round cadence — daily 30 minutes, or three longer rounds a week?
2. **How many live topics can one round carry before it stops being a
   discussion?** That number, not the intake rate, is our real capacity.
3. Two days of understanding before the scope meeting fits a short project.
   What replaces it when the research alone has historically taken weeks — a
   longer day-2, or an explicit research phase with its own end date?
4. Does everyone attend every round — is it our only scheduled meeting?
5. Who decides that two reports are one topic, and when?
6. Who becomes lead — rotation, whoever triaged it, or the area owner? And can
   a project lead also drive topics inside that project, or is that one head
   too many things?
7. Maintenance arriving mid-project: does the project team absorb it, or do we
   keep someone out of the project — which breaks the 100% rule?
8. PM says the agreed scope no longer fulfils the PRD and the team disagrees —
   who breaks the tie?
9. Do routine bulk changes need a **fast lane**: no design note, AI states the
   invariant it preserved and how it proved it, review is of the invariant?
10. Where do external contributor PRs enter — at review, or at the problem?
11. `[Hilla]` forum posts belong to another repo — file by tag, react only to
    `[Flow]`, or let triage re-route?
12. Is one weekly spot-check enough — per team, or per person?
13. Design notes for bugfixes too, or only for enhancements?
