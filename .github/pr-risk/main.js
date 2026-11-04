/**
 * Entry point of the PR risk workflow, called by actions/github-script with
 * its authenticated `github` client, `context` and `core`. Classifies the pull
 * request of the event, reports the category as a check run on its head
 * commit, sets one of the risk labels and posts or updates one comment that
 * explains it.
 */
import { assess as defaultAssess } from './assess.js';
import { buildDiff, classify, evaluatePolicy, isTrustedAuthor, isTrustedHead, renderComment } from './classify.js';

export const MARKER = '<!-- pr-risk -->';

export const LABELS = {
  green: {
    name: 'risk: green',
    color: '0e8a16',
    description: 'Low risk: can merge after CI and the agent review pass',
  },
  amber: { name: 'risk: amber', color: 'fbca04', description: 'Bounded risk: needs one human approval' },
  red: { name: 'risk: red', color: 'b60205', description: 'High risk: needs expert review' },
};

const COMMENT_AUTHOR = 'github-actions[bot]';

/**
 * The check run that binds the category to the commit it was made for. Unlike
 * a label, only a GitHub App can create one. A required check passes on
 * `neutral` too, so amber and red pull requests can still merge after review.
 */
export const CHECK_NAME = 'PR risk';
const CHECK_CONCLUSIONS = { green: 'success', amber: 'neutral', red: 'neutral' };

async function ensureLabel(github, repo, label) {
  try {
    await github.rest.issues.createLabel({ ...repo, ...label });
  } catch (error) {
    // 422: the label exists already
    if (error.status !== 422) {
      throw error;
    }
  }
}

async function removeLabel(github, repo, pr, name) {
  try {
    await github.rest.issues.removeLabel({ ...repo, issue_number: pr.number, name });
  } catch (error) {
    // 404: someone removed the label meanwhile
    if (error.status !== 404) {
      throw error;
    }
  }
}

/** Replaces the risk label among the `current` label names */
async function setLabel(github, repo, pr, current, category) {
  const label = LABELS[category];

  // Remove before adding, so that a run that stops halfway leaves no label
  // rather than green next to the label that replaces it
  for (const { name } of Object.values(LABELS)) {
    if (name !== label.name && current.includes(name)) {
      await removeLabel(github, repo, pr, name);
    }
  }
  if (!current.includes(label.name)) {
    await ensureLabel(github, repo, label);
    await github.rest.issues.addLabels({ ...repo, issue_number: pr.number, labels: [label.name] });
  }
}

async function upsertComment(github, repo, pr, body) {
  const comments = await github.paginate(github.rest.issues.listComments, {
    ...repo,
    issue_number: pr.number,
    per_page: 100,
  });
  const existing = comments.find(
    (comment) => comment.user?.login === COMMENT_AUTHOR && comment.body?.startsWith(MARKER),
  );
  if (existing) {
    await github.rest.issues.updateComment({ ...repo, comment_id: existing.id, body });
  } else {
    await github.rest.issues.createComment({ ...repo, issue_number: pr.number, body });
  }
}

/**
 * Classifies the pull request of the event. The steps that do not depend on
 * each other run at the same time, as each one waits on a network call.
 * `isMember` tells whether the author is a member of the organization; the
 * workflow checks it in an earlier step.
 */
export async function run({
  github,
  context,
  core,
  env = process.env,
  assess = defaultAssess,
  isMember = () => Promise.resolve(env.AUTHOR_IS_MEMBER === 'true'),
}) {
  const repo = context.repo;
  const pullNumber = context.payload.pull_request.number;
  const runUrl = `${context.serverUrl}/${repo.owner}/${repo.repo}/actions/runs/${context.runId}`;

  // Read the pull request again: a run that waited in the queue would act on a stale event
  const [{ data: pr }, files] = await Promise.all([
    github.rest.pulls.get({ ...repo, pull_number: pullNumber }),
    github.paginate(github.rest.pulls.listFiles, { ...repo, pull_number: pullNumber, per_page: 100 }),
  ]);
  if (pr.state !== 'open' || pr.draft) {
    core.notice(`#${pullNumber} is closed or a draft, skipping`);
    return;
  }

  // A green label must not stay on code that this run has not assessed yet.
  // The removal starts before the assessment and ends before the labels are set.
  let labels = pr.labels.map(({ name }) => name);
  let greenRemoved = Promise.resolve();
  if (labels.includes(LABELS.green.name)) {
    greenRemoved = removeLabel(github, repo, pr, LABELS.green.name);
    labels = labels.filter((name) => name !== LABELS.green.name);
  }

  const repository = pr.base.repo.full_name;
  const diff = buildDiff(files, { repository });
  const assessing = assess({ pr, files, diff, env }).then(
    (assessment) => ({ assessment, failure: null }),
    (error) => ({ assessment: null, failure: `The agent assessment failed: ${error.message}` }),
  );

  const authorTrusted = isTrustedAuthor({
    authorAssociation: pr.author_association,
    isMember: await isMember(pr.user.login),
  });
  const policyFindings = evaluatePolicy({
    repository,
    title: pr.title,
    body: pr.body,
    authorTrusted,
    headTrusted: isTrustedHead(pr),
    files,
  });

  const [{ assessment, failure }] = await Promise.all([assessing, greenRemoved]);

  const result = classify({ policyFindings, omitted: diff.omitted, assessment });
  const body = renderComment({ marker: MARKER, result, assessment, headSha: pr.head.sha, runUrl });

  await github.rest.checks.create({
    ...repo,
    name: CHECK_NAME,
    head_sha: pr.head.sha,
    status: 'completed',
    conclusion: CHECK_CONCLUSIONS[result.category],
    external_id: String(context.runId),
    details_url: runUrl,
    output: { title: `${result.category[0].toUpperCase()}${result.category.slice(1)}`, summary: body },
  });
  await setLabel(github, repo, pr, labels, result.category);
  await upsertComment(github, repo, pr, body);
  await core.summary.addRaw(body).write();
  core.setOutput('category', result.category);

  // The label is still set from the policy alone, which is never green, but
  // the run fails so that a lost assessment does not go unnoticed
  if (failure) {
    core.setFailed(failure);
  }
}
