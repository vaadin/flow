# Release digest

A Slack digest in **#flow-dev** that answers "which Flow branches need a release
this week?" so nobody has to work it out by hand.

Built by [`release-digest.yml`](workflows/release-digest.yml), which is run from
the Actions tab. It is meant to become a Monday-morning weekly, and the workflow
carries the `cron` to add for that; until then nothing runs on its own.
Every run writes the message it built into the job summary, posted or not.

## What it reports

One entry per branch that has unreleased work, always including `main`:

- the branch and the version it would be released as (`25.2.6` → `25.2.7`,
  `25.3.0-alpha7` → `25.3.0-alpha8`);
- how many unreleased commits there are, split into feat/fix, dependency bumps,
  and everything else;
- the commit subjects, feat/fix first, with a link to the full diff;
- whether the changes look worth releasing, and why.

Branches with nothing worth releasing collapse into a single closing line.

## How it decides

**Branch list.** Read from `on.push.branches` in
[`validation.yml`](workflows/validation.yml), so CI remains the single source of
truth. `main` is added by the digest and its version line comes from its POM
version, because pre-releases are tagged `25.3.0-alphaN`, not `main.N`.

**Commit classification** ([`checkReleases.js`](../scripts/checkReleases.js)) —
every commit since the branch's latest tag, by conventional-commit type:

| Category     | Types                              | Means                                  |
| ------------ | ---------------------------------- | -------------------------------------- |
| `releasable` | `feat` `fix` `perf` `revert` `foo!`| On their own justify a release         |
| `deps`       | `chore(deps)` and similar scopes   | Worth releasing in a batch, rarely alone |
| `internal`   | `refactor` `docs` `test` `ci` …    | Do not justify a release               |

That yields a verdict of `release`, `consider`, or `skip` from commit types
alone.

**Release-worthiness judgement**
([`releaseDigest.js`](../scripts/releaseDigest.js)) — the commit subjects go to
Claude, which recommends `release` / `consider` / `skip` per branch with a
one-sentence rationale. It exists because the type prefix is not always the
truth: a `fix:` that only touches tests is not a user-visible fix, and a
`chore(deps)` closing a CVE is worth shipping.

The judgement is **advisory**. It is shown next to the commit counts, and where
it disagrees with the type-derived verdict the digest says so rather than hiding
one behind the other. Without `ANTHROPIC_API_KEY`, or if the call fails, the
digest still posts using commit types alone.

## Setup

The digest posts through a **Slack Workflow Builder** webhook trigger, because
the Vaadin workspace does not allow creating Slack apps. That trigger's message
step inserts a variable's value as text without parsing Slack markup, which is
why the digest is plain text with literal emoji and bare URLs.

In Slack: **Tools → Workflow Builder → New Workflow**, start it *From a webhook*,
declare one variable named `digest` of type **text**, add a **Send a message**
step to #flow-dev whose entire body is that variable, then **Publish** and copy
the trigger URL (`https://hooks.slack.com/triggers/...`).

### Trying it before Slack is wired up

A manual run does not post unless its **Post to Slack** input is ticked, so the
workflow can be merged and run with no secret configured: the run stays green
and the job summary shows the branch table and the exact message that would have
been sent. Ticking the box, or adding the schedule, makes the run post — and
fail loudly if it cannot.

### The secret

Then one repository secret:

| Secret                         | Purpose                                                |
| ------------------------------ | ------------------------------------------------------ |
| `SLACK_RELEASE_DIGEST_TRIGGER` | The published trigger's URL                            |
| `ANTHROPIC_API_KEY`            | Optional; already present for the Claude Code workflow |

The variable name is the workflow's `SLACK_TRIGGER_VARIABLE`; the payload keys
must match the workflow's declared variables exactly or Slack rejects the post.

## Changing it

- **Branches** — edit `on.push.branches` in `validation.yml`; the digest follows.
- **Schedule** — add the `cron` shown in `release-digest.yml`.
- **Channel** — edit the Send a message step in the Slack workflow.
- **What counts as releasable** — `RELEASABLE_TYPES` in `checkReleases.js`.
- **How strictly to judge** — `SYSTEM_PROMPT` in `releaseDigest.js`.

## Running it locally

```bash
node ./scripts/checkReleases.js --out=release-check --include-main
node ./scripts/releaseDigest.js --in=release-check --no-post
```

The digest goes to stdout either way, so `--no-post` prints exactly what would
have been sent — that is what a manual workflow run does. Add `--no-fetch` to
the first command to work from local refs instead of fetching, and unset
`ANTHROPIC_API_KEY` to skip the model call.

To post for real, drop `--no-post` and set `SLACK_TRIGGER_URL` — point it at a
test channel's own trigger first.

## Cost and failure modes

Each run's model call is a few thousand input tokens and well under a cent. A
Slack or model outage fails the run loudly in the Actions tab; it never posts a
partial digest. Missing tags on a branch are reported as "since start of branch"
rather than failing the run.
