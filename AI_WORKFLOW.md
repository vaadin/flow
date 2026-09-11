# AI-First Workflow for the Flow Team

**Status:** draft for team discussion.

Developers spend their time on **problems, design and decisions**. AI does the
**research, drafting, implementation, tests and revisions**.

**The process starts when a GitHub issue appears.** A human files it — deciding
that something deserves the team's attention is the entry ticket — and from that
moment automation moves it: a brief and a draft PR within the hour, revisions
after every decision. What the team supplies is decisions.

Two layers: how a **project** runs (a PRD becomes a shipped increment, §4) and
how an **issue** moves (a filed issue becomes a merged PR, §5–6). Our conventions
and guidelines hold the content of how we build; this holds the process.

**Deliberately not covered yet:** where issues come from — turning threads and
support tickets into issues, noticing that five of them are one problem, or
letting automation file its own. This document starts from an issue that exists,
because that is where we lose the weeks.

---

## 1. Why

Four things eat most of our week: **research and design from a one-sentence ask**
— days, sometimes months, of locating where it lands in Flow and reconstructing
how that area works today; **reading the diff** line by line for mechanical
correctness; **reviewing routine bulk changes** — bumps, refactors, migrations;
and **impact analysis repeated per issue** — is this needed, what can it break.
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

## 2. People file, automation moves, we decide

Automation is not a helper we invoke when we remember to. A new issue triggers
it, and it keeps going until it needs a decision — then it stops and says so.

**Automation does this without being asked:** reads the issue and everything
linked from it; files it in the right area; points at likely duplicates; writes
the Analysis Brief with a proposed verdict; opens a draft PR with a reproducing
test and the sketched API; keeps CI green; revises on comment; drafts docs, demo
and DX tests; keeps the board truthful; and writes the daily digest (§5).

**What starts it is a state, not a person remembering.** A filed issue starts the
brief and the probe; an accepted problem starts the design note; an agreed design
starts the finished implementation; a question in the thread starts a revision.
The state of an issue is a switch rather than a sticker on a board — which is
also why the board cannot drift away from what is happening.

**A human decides three times per issue:**

| # | What is decided | Where | State after |
| --- | --- | --- | --- |
| 1 | Is this a real problem, and is it ours? (not: what do we build) | the daily (§5) confirms what AI proposed | accepted · closed · parked · waiting on the reporter |
| 2 | What is the design — the probe's shape, or another? And how do we build it? | the design session (§5) | design agreed |
| 3 | Do we merge, and who owns it afterwards? | async by default; the daily when it is disputed | merged |

Between the second and the third nothing is asked of anyone: AI is finishing the
work, and we speak only if it is blocked. Everything around those three decisions
is automation's. If an issue needs a fourth, that is a signal — either the brief
was thin or the design was never settled; say which, in the issue.

**Automation never** merges, never closes an issue as won't-fix, never declares
two issues duplicates on its own, never changes an agreed API contract, and
never rules its own deviation acceptable. The agent that wrote a change has no
route to approving it.

**Nothing safe waits for permission:** building, testing and formatting are
approved in advance, because an agent idling on a prompt costs what a PR idling
on a reviewer costs. And everything automation produces before decision 2 is
**disposable** — which is exactly what makes it safe to produce early.

---

## 3. Roles

A role is two things: **what you owe, and what others may demand of you.** We
usually write down only the first, which is why people know their tasks but not
what to expect from each other.

| Role | Owes | Others may demand |
| --- | --- | --- |
| **PM** — stakeholder, owns the PRD, never 100% on one project | Who the user is, what problem, why now, what success looks like; written answers within a day | Clarity about the *problem*, and a straight answer on whether something still serves it |
| **The team** — everyone else, 100% allocated | The shipped increment: design, code, tests, docs, demo, DX, quality, usability. Deciding *how*, and *how little* | That it decides and ships without being chased, and asks when the PRD is ambiguous |
| **Lead** — one per piece of work, on the team; *not* its implementer. The **project lead** on a project, the **issue lead** on a single issue — same role, different scope | That the work reaches decisions and goes in the right direction at the right pace: a prepared meeting, a truthful board, nothing left unowned, blocked people unblocked | Discussion opened early, an agenda before the meeting rather than at it, specific questions, a straight answer on what matters most right now, gaps named early rather than discovered late |
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
  the design session does.
- **The team connects the PRD to the work.** Nobody else spans that gap.
- **Nobody is "partly" on the team.** Partial membership is worse than absence:
  it blurs who owes what and leaves work half-done. If you cannot be 100%, you
  are a consulted expert — a real role with clear expectations.

**The lead** carries one piece of work to the end. The lead does not build it —
AI does — and is accountable for the outcome rather than for having typed it, and
does not decide the design alone: that happens in the session. A lead often is
not the person with the most scar tissue in that area, and that is the point —
it forces context out of one head. Concretely:

- **Prepares every meeting.** Reads the digest, each brief and each draft PR
  beforehand and turns it into one question with a recommendation. The pre-read
  is AI's job, the agenda is the lead's — **no agenda, no meeting.**
- **Opens the discussion at revision 1** — a polished revision 4 reads as a fait
  accompli and gets worse input — and asks sharp questions instead of open ones:
  "should detach cancel the pending update or queue it?" gets a decision where
  "thoughts?" gets silence.
- **Watches direction and pace**, not progress: right thing, right order, nothing
  unowned, nobody quietly stuck for two days — said early, not at the end.
- **Keeps the board honest**, pulls people in by name, records every conclusion
  in the issue, and decides alone only between meetings, marked as unilateral.

Leads rotate. On a project the lead is fixed for its duration.

---

## 4. Running a project

**A product is what the user can do, not what we implemented.** So the unit of
planning is a **use case**; *done* means a user can do it — API that reads well
from outside, docs, sane errors, a demo — not "the PRs are merged"; progress is
reported as capabilities gained; and quality and usability are ours, because
they are the product.

**Kickoff (day 0).** The PM presents the PRD. The team asks anything. **No
estimates, no scope commitment** — nobody understands the problem yet. Output:
the roster (100% each) and the first written questions to the PM.

**Days 0–2.** AI produces an Analysis Brief (§6) per candidate use case: where
it lands, how that area works today, what supporting it would cost.

**Day 2 — the scope meeting.** The team agrees the **Use Case List**: what we
will support, and what we identified and deliberately will **not**, one line of
why each. The second list carries as much weight as the first — it makes
"minimal" checkable and stops us re-litigating scope on day nine. The PM says
whether the scope still fulfils the PRD; the team decides what it builds. Every
use case on the list is an issue, and moves like any other one (§5–6).

**Minimal by default.** Ideas that emerge mid-project go to the **parking lot** —
one line, who raised it, why it looked good — and **nobody starts on a parked
idea**; at the end it is filed as an issue like anything else. Good ideas are not
the problem; good ideas started quietly are.

**Rituals.** The project runs its own daily and design session (§5), and the PM
sees a working walkthrough weekly — to confirm we are solving the right problem,
not to accept or reject the work.

**The board is public and truthful.** At any moment anyone can see what is in
progress, what is done, and what nobody has picked up; knowing what to do next
should never require asking someone.

**Done does the reminding.** Demos, docs and DX tests get dropped because they
are tracked apart from the code and postponed one day at a time. So they belong
to the use case's definition of done — a use case with merged code and no docs is
*not done*, and the board shows it that way. Nobody chases work that cannot be
marked complete without it, and the lead's job is the part structure cannot do
(§3): whether we are building the right thing in the right order.

---

## 5. The issue, the daily and the design session

**The issue is the unit.** One issue is one problem, and the brief, the design
note, every decision and the PR all hang off it; duplicates are closed onto it
rather than discussed twice. It is named as **the problem in the user's words** —
*"Grid loses selection after a refresh"*, not *"add a keepSelection flag"*.

It stays **one continuous discussion, from the problem to the merge decision.**
The issue holds the problem and the decisions, the PR holds everything that
changes as the work does, and both exist from the first hour — so the
conversation never moves house and restarts with half its context. The brief and
the design note are documents in that PR rather than comments, so revision 4
arrives as a diff against revision 3. They stay after the merge: a searchable
answer to "why does this API read like this", and the raw material for
harvesting (§6 stage 7).

**The daily — 60 minutes, the whole team, in two halves.** The first half is a
round table: everyone takes a turn on one thing — a PR they opened, a PR they are
reviewing, or what they are working on right now. One item each, not a tour of
everything. The second half walks the day's new issues and PRs, confirming the
verdicts AI proposed and stopping only where somebody objects, is blocked, or
raises a question that concerns everyone.

**The digest is what makes the second half possible.** AI writes it before the
daily, and it is a list of decisions rather than a news feed:

- **decide today** — a handful of items, each with the proposed verdict and one
  line of why;
- **stuck** — what has waited longer than it should, and who owns it;
- **for information** — everything else, read on your own, never read aloud.

It also routes: a PR that touches the area of an earlier one says so, with the
name of whoever reviewed that one. That is how work someone has already touched
finds the person who touched it, without anyone reporting it.

**The design session — once a week, two blocks of 45 minutes with a break.**
Anything with design content goes to a *needs a design* column and waits for the
session; three or four topics, pre-read published a day ahead. Design is the
decision we least want taken in a hurry, and the one place where the whole team
in one room is worth what it costs. Between sessions the lead may settle a small
design question alone, marked as unilateral — that is what keeps a two-minute
question from waiting a week.

During a project the daily and the design session are the project's, and members
skip their home team's ceremonies — two rhythms is what makes 100% impossible.

- **Problem before solution, always** — *even though a PR is already open.* The
  draft PR is evidence about the problem, not a proposal awaiting approval. An
  issue may not be discussed as an implementation until the team has said out
  loud what the problem is. Most disagreements about *how* are unnoticed
  disagreements about *what*.
- **No unprepared meeting.** Everyone arrives having read the digest or the
  pre-read; the lead arrives with an agenda — one question per item, each with a
  recommendation. A meeting without one is moved, not endured.
- **One item per person, and only what someone can act on.** A PR: explain in a
  few sentences what it does, so whoever reviews it starts warm. A new issue:
  explain the problem, so the team recognises it when it comes back. A piece of
  research: ask the question you are stuck on. What the turn is not is a long
  account of your week — when nothing is required of the room, the room stops
  listening, and everything that changes nothing for anybody is in the digest
  already.
- **Design and implementation in one pass.** The same session settles the design
  *and* the approach, so AI goes straight from it to a finished PR. Splitting
  across two sessions is the exception, for genuinely new ground.
- **Two or three issues per person, then stop adding.** Running more sessions is
  nearly free; reading what they produce is not. The limit is the person
  steering, not the machine.

| Step | Target | Limit |
| --- | --- | --- |
| Issue filed → brief + draft PR | 30 min | 2 h |
| Brief → first daily | next daily | 1 day |
| Problem agreed → design agreed | next design session | 2 sessions |
| Design agreed → PR ready for review | 1–4 h | 1 day |
| Ready → merge decision | 1 day | next daily |
| **Filed → merged, no design needed** | **2 dailies** | **1 week** |
| **Filed → merged, design needed** | **1 week** | **2 weeks** |

Counting in meetings is deliberate: "this has waited two design sessions" is
harder to ignore than "it has been a couple of weeks".

---

## 6. The pipeline

```mermaid
flowchart TD
    S0["Stage 0 · AN ISSUE IS FILED<br/>by a human — that is the entry ticket"]
    S1["Stage 1 · BRIEF + DRAFT PR<br/>AI, ~30 min<br/>context · verdict · sketch · a probe that compiles"]
    S2{"Stage 2 · TRIAGE<br/>daily: what is the problem, is it ours?"}
    RJ["close — issue and probe; the brief is the answer"]
    NI["question back to the reporter"]
    PK["parked — the brief stays, the probe is closed"]
    S3{"Stage 3 · DESIGN + APPROACH<br/>design session: this shape, or another?<br/>AI drafts · team agrees · design agreed"}
    S4["Stage 4 · IMPLEMENTATION<br/>AI · the same PR grows up<br/>tests first · green CI · ready for review"]
    S5{"Stage 5 · REVIEW<br/>do we merge?"}
    S6["Stage 6 · MERGE<br/>a human owns it"]
    S7["Stage 7 · HARVEST<br/>what would have made this cheaper<br/>→ a rule, a block, or a check"]

    S0 --> S1 --> S2
    S2 -- rejected --> RJ
    S2 -- needs info --> NI
    S2 -- parked --> PK
    S2 -- accepted --> S3 --> S4 --> S5
    S5 -- comments --> S4
    S5 -- design wrong --> S3
    S5 -- approved --> S6 --> S7

    classDef decision stroke-width:3px;
    class S2,S3,S5 decision;
```

The three thick-bordered stages are the three decisions (§2). Everything else
happens between meetings, without us.

**0 · The issue.** Anyone files it — team, support, PM, a user. What a filer owes
is **the problem, not a solution**; a proposed API is welcome as a hint, but the
first line has to say what somebody could not do.

**1 · Brief and draft PR** (AI, ~30 min). One reaction, two artefacts:

- the **Analysis Brief**, committed as a document in the probe PR and linked from
  the issue — context, verdict, sketch, and what it could not verify. It is the
  pre-read that makes a decision possible;
- a **draft PR** — a probe: a test that reproduces the problem (failing), the
  sketched API compiling, and CI showing what else moves.

**Why the PR exists before any decision.** A brief can claim "two lines in one
class" and be wrong; a branch that compiles says what the change actually costs,
and CI turns blast radius from an estimate into a list of names. It also gives
the team something concrete to react to, and reacting is far easier than
originating (§10). If the shape survives the design session we are already at
review;
if it does not, we close a branch — the cheapest artefact we produce. The probe
says in its own description what it does *not* settle, and the brief still
carries the alternatives AI did not build — otherwise the one shape that exists
wins by default.

**2 · Triage** — AI proposes a verdict in the brief and the daily confirms it in
bulk, stopping only where someone objects. The question is shallow on purpose:
"worth our design time?", not "is this right?". Rejecting closes the probe with
the issue — an ordinary Tuesday, not waste.

**3 · Design and approach** — the brief grows into the **design note** in the
same file, so every revision is a diff with a one-line "what changed and why",
and the current version is the file rather than the newest comment. The note is
what the team argues about; the probe is exhibit A, not the proposal.
*"Rework it: use an event instead of a callback, and define what happens on
detach."* Every conclusion lands back in the note. **Right problem, wrong
shape** is a first-class outcome: the probe is discarded and the next revision
starts from the design, not from the code that happens to exist.

**4 · Implementation** (AI) — the same PR grows up against the agreed note: tests
first (if they expose a design problem, **go back to Stage 3** rather than bend
the tests), green CI, a description reviewable without the diff, and a line on
**which parts of the probe survived the decision** — code that is there because
it was there on day one is the failure mode of starting early.

**A deviation from the note is corrected in the note, in the same commit**, so a
change of plan arrives as a diff anyone can see rather than a paragraph at the
bottom of a description. Silent deviation is still the worst failure mode of this
process; it is now also the easiest to spot.

**Nothing reaches a human before the check has run.** Every issue has one
verification target and one quantifiable statement of done — the tests green, the
failing test from stage 1 now passing, compatibility unbroken — and AI shows the
output rather than asserting it. A fix therefore starts with a failing test, and
that test is then off limits: edits to it are blocked for the rest of the task,
so the check that proves the bug cannot be weakened into agreement. If the target
cannot be stated at all, the gap is in Stage 3, not here.

**5 · Review** — read the description: does it solve the problem? Is the API what
we agreed, and what we want to live with? What happens at the edges — null,
detach, concurrency, serialization, back-compat? What is the blast radius?
Comment in the PR and AI revises; reviewers do not push fixes themselves, because
asking keeps the rule harvestable. AI then carries the PR to the gate on its own,
sweeping unresolved comments and red checks until everything is green, and waits
there — the approval is not its to give. Anything with design content is decided
in the design session by the people who agreed the design; small and routine
changes async. Bouncing back to Stage 3 is a success, not a failure.

**6 · Merge** — approving means *"I understand this and I am comfortable owning
it."* Never approve to unblock someone: "the AI wrote it and CI was green"
explains nothing.

**7 · Harvest** — every merge ends with one question: *what would have made this
cheaper?* The answer becomes one of three things, and choosing the right one
matters more than the writing:

| Strength | Form | Use when |
| --- | --- | --- |
| advisory | a written rule or guideline AI reads before working | the default is clear, and real exceptions exist |
| enforced | an automatic block before the action | it must hold every time, and a violation is recognisable in advance |
| proved | a check that runs on every change | it must hold every time, and only running the code can show it |

**Prose cannot hold a rule that must always hold.** A convention that admits no
exception belongs in a block or a check; writing it down again, in bolder words,
is what we do instead of fixing it. A recurring piece of analysis becomes a
standing instruction AI loads by itself. And because all of this is what steers
AI, changing it is a change that gets tested — §9. This is what makes the next
cycle shorter than this one.

---

## 7. The four artefacts

Formats belong somewhere else; what matters here is what each has to answer, and
that the next stage reads the previous one instead of starting over.

**The brief** — where this lands in Flow and how that area works today; a verdict
with the alternatives to accepting it, including solving it outside Flow; a
sketch of the API and what it touches; and, kept separate, what AI verified
versus what it assumed.

**The probe PR** — the problem in one paragraph; what the probe demonstrates; what
it does *not* settle; and the condition that would make this shape wrong.

**The design note** — the brief, one revision later: the problem in the user's
terms, goals and non-goals, the design and its contracts, behaviour at the edges,
how we intend to build it, and the alternatives we rejected with the reason for
each. That last part is the point of the document.

**The PR description** — enough to review without opening the diff: what and why,
the design it was built against, which piece does what, what survived from the
probe, the evidence that the check passed, what is tested and deliberately not,
and the blast radius.

Several mechanics here come from Anthropic's [AI-native SDLC
playbook](https://claude.com/blog/the-ai-native-sdlc-playbook), which keeps three
documents per change where we keep two.

---

## 8. What replaces line-by-line review

The real gate is the design discussion; the automated gates — build, tests,
formatting, API compatibility — are a precondition for review, not its outcome.
On top of that, **a human still reads the code**, and the PR is marked as
needing that, when it touches:

- public API surface;
- security-sensitive paths: request handling, session, class-loading,
  deserialization, path resolution;
- the client-server protocol, anything crossing the wire;
- concurrency and push;
- performance-critical paths, or any change justified by performance;
- anything AI flagged as uncertain, or that deviates from the note.

**Before any of that, AI reviews the PR**, and what it looks for is written down
rather than improvised per reviewer: the passes it makes — correctness,
security, the protocol and serialization, public API and back-compatibility, our
own conventions — the line between *important* and *nit*, a cap on nits per PR,
and what not to look at at all (generated sources, and anything the automated
gates already enforce). The findings inform the humans; they neither approve nor
block, and the agent that wrote the change has no route to approving it.

Plus **one random PR per week, read in full.** This is our calibration: it tells
us whether the descriptions we trust match the code. A mismatch is a process
incident — discuss it and fix the rule, do not quietly fix the PR, and turn that
case into a standing task in the eval suite (§9), so it cannot come back
unnoticed.

---

## 9. Testing what steers AI

Our conventions, our guidelines, the standing instructions AI loads for a task,
the automatic blocks — none of that is documentation. It is the program that
decides how every change here gets written, and we edit it weekly with no idea
what the edits do: a rule added for one awkward case can quietly make ten
ordinary ones worse, and we find out a month later, by accident.

So **what steers AI is tested like code.** The tests are called evals; the set of
them is the suite.

- **A few dozen real tasks**, taken from issues we have already closed: the issue
  as it arrived, and what a good answer looks like — tests pass, the convention
  is followed, the API matches what we merged, nothing unrelated is touched.
- **It runs whenever we change what steers AI**, and on a schedule too, because
  the models change under us even when our own rules do not.
- **The pass rate is a merge signal.** A rule that fixes one task and breaks four
  is visible before it is merged. That is the whole point.
- **Every process incident becomes a permanent task in it:** the weekly
  spot-check mismatch (§8), the bug that got through review, the convention AI
  kept ignoring.

It is also the honest answer to *"is harvesting working?"* — Stage 7 adds rules,
this is what tells us a rule did what we hoped rather than making us feel
organised.

---

## 10. Culture

- **Design is discussed, code is generated.** Code existing early does not make
  it the design. Arguing about code in a PR means we skipped a design
  discussion.
- **A PR must be reviewable without the diff.** If it is not, the description is
  the defect.
- **A human always decides** — three times per issue (§2), and never fewer.
  Never approve what you do not understand, and own the merge afterwards.
- **Correct the constitution, not just the output.** Fixing the same thing twice
  by hand means we forgot Stage 7 — and a correction nobody tested is a hope,
  not a rule (§9).
- **Reward good rejections.** An issue closed in an hour with a clear explanation
  is a first-class outcome.
- **No silent local rewrites.** If you take an issue over and write it yourself,
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
- **Nobody defends "their" design, or their probe.** It belongs to the team the
  moment it is on the screen — that is what makes it safe to attack, and safe to
  be wrong. If probes are almost never discarded, we are not designing; we are
  approving the first thing that compiled.
- **Everyone speaks before anyone concludes.** Passing is allowed; staying
  invisible is not. An issue that passes through a meeting in silence was decided
  by whoever spoke last, not by the team.
- **Nobody arrives cold.** Silence usually means nobody has context, and
  reacting is far easier than originating — which is what the brief and the probe
  are for. No brief, no design discussion; no agenda, no meeting.
- **Written threads are first-class**, not a fallback: the quietest person in a
  call often writes the sharpest thing in the thread.
- **A silent meeting is not diligence.** Ten minutes of only the lead talking
  ends the meeting; rebook it with a pre-read.

---

## 11. Adoption, signals, open questions

Adopt the **project layer (§4) whole** on the next project — roles, allocation
and rituals only work as a set. The **issue layer** can be phased: two weeks of
briefs only, then briefs plus probe PRs, then design notes, then the full
pipeline with spot-checks and a written review policy, and the eval suite (§9) as
soon as we start changing what steers AI weekly — which is immediately. Widen
only while the spot-check mismatch rate stays low, and review this document at
the end of each phase.

**What to watch.** Fast signals say the process is moving; slow ones say it was
worth moving. A fast signal that looks good while its slow partner rots is the
thing to catch.

| What | Fast — visible this week | Slow — visible over months |
| --- | --- | --- |
| Filing → decision | hours from filing to brief and probe · brief to first daily | issues that needed a fourth decision |
| Design | share agreed in the first session that saw them · **probes discarded at design** (near zero means we rubber-stamp the first shape) | bounces back to design after review |
| Implementation | first-pass CI success · issues one person steers at once while review holds | rework per merged change |
| Review | time to the first AI review · comments resolved without a human touching the branch | defects found before merge vs. after release |
| Steering files | eval pass rate when a rule changes · time from a process incident to an eval | **spot-check mismatch rate** (the honesty metric) · rules added per month |
| Project | use cases done vs. agreed on day 2 | how much scope we managed *not* to build · how many people spoke |

Still to decide:

1. **How many live issues can one daily carry before it stops being a
   discussion?** That number, not the filing rate, is our real capacity. The
   same limit applies to the round table: when turns start sounding like a tour
   of the week, the first half is what we cut, not the second.
2. Does *every* new issue get a probe, or only ones that pass triage — and when
   is a stale probe closed, by whom?
3. Who owns an issue in its first hour, before the first daily sees it?
4. Who owns the eval suite, and how big can it get before it is too slow to run
   on every change?
5. Two days of understanding before the scope meeting fits a short project. What
   replaces it when the research has historically taken weeks?
6. Who becomes lead — rotation, whoever triaged it, or the area owner? Can a
   project lead also lead issues inside that project?
7. Maintenance arriving mid-project: does the project team absorb it, or do we
   keep someone out — which breaks the 100% rule?
8. PM says the agreed scope no longer fulfils the PRD and the team disagrees —
   who breaks the tie?
9. Do routine bulk changes need a **fast lane**: no design note, AI states the
   invariant it preserved and how it proved it, review is of the invariant?
10. Where do external contributor PRs enter — at review, or back at the problem?
11. Design notes for bugfixes too, or is a probe with a failing test enough?
