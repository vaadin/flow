/**
 * Entry point of the PR risk workflow, called by actions/github-script with
 * its authenticated `github` client, `context` and `core`. Classifies the pull
 * request of the event and posts or updates one comment that explains its
 * risk level.
 */
import { assess as defaultAssess } from './assess.js';
import {
  buildDiff,
  classify,
  evaluatePolicy,
  isTrustedAuthor,
  isTrustedHead,
  linkedIssueRefs,
  renderComment,
} from './classify.js';

export const MARKER = '<!-- pr-risk -->';

const COMMENT_AUTHOR = 'github-actions[bot]';

/**
 * The issues that the pull request links, as `{ ref, title, state, labels }`.
 * An issue that cannot be read, or a reference to a pull request, is skipped:
 * the linked issues only add information and never decide alone.
 */
async function readLinkedIssues(github, repo, pr) {
  const issues = await Promise.all(
    linkedIssueRefs(pr.body, repo).map(async ({ owner, repo: name, number }) => {
      try {
        const { data } = await github.rest.issues.get({ owner, repo: name, issue_number: number });
        if (data.pull_request) {
          return null;
        }
        return {
          ref: `${owner}/${name}#${number}`,
          title: data.title,
          state: data.state,
          labels: data.labels.map((label) => (typeof label === 'string' ? label : label.name)),
        };
      } catch {
        return null;
      }
    }),
  );
  return issues.filter(Boolean);
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
 * Classifies the pull request of the event. `isMember` tells whether the
 * author is a member of the organization; the workflow checks it in an
 * earlier step.
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

  const diff = buildDiff(files);
  const linkedIssues = await readLinkedIssues(github, repo, pr);
  const authorTrusted = isTrustedAuthor({
    authorAssociation: pr.author_association,
    isMember: await isMember(pr.user.login),
    isBot: pr.user.type === 'Bot',
  });

  // Anyone can open a pull request from a fork and push to it again and again,
  // and each assessment is paid for. Pull requests of authors outside the
  // organization need a human approval anyway, so the rules alone decide.
  const skipped = !authorTrusted;
  let assessment = null;
  let failure = null;
  if (!skipped) {
    try {
      assessment = await assess({ pr, files, diff, linkedIssues, env });
    } catch (error) {
      failure = `The agent assessment failed: ${error.message}`;
    }
  }

  const policyFindings = evaluatePolicy({
    author: pr.user.login,
    title: pr.title,
    body: pr.body,
    authorTrusted,
    headTrusted: isTrustedHead(pr),
    files,
    linkedIssues,
  });
  const result = classify({ policyFindings, omitted: diff.omitted, assessment, skipped });
  const body = renderComment({ marker: MARKER, result, assessment, skipped, headSha: pr.head.sha, runUrl });

  await upsertComment(github, repo, pr, body);
  await core.summary.addRaw(body).write();
  core.setOutput('level', result.level);

  // The comment still gives the level from the rules, but the run fails so
  // that a lost assessment does not go unnoticed
  if (failure) {
    core.setFailed(failure);
  }
}
