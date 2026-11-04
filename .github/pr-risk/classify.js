/**
 * Pure functions of the PR risk workflow: evaluate the repository policy,
 * build the diff that the agent reads, combine the policy with the agent's
 * assessment into the final category, and render the PR comment.
 */
import { matchesGlob } from 'node:path';
import { BREAKING_BODY, BREAKING_TITLE, CONTENT_RULES, policyFor, TRUSTED_ASSOCIATIONS } from './policy.js';

/** From lowest to highest risk */
export const CATEGORIES = ['green', 'amber', 'red'];

export const DIMENSIONS = [
  { key: 'impact', title: 'Impact' },
  { key: 'blast_radius', title: 'Blast radius' },
  { key: 'uncertainty', title: 'Uncertainty' },
  { key: 'reversibility', title: 'Reversibility' },
];

const CATEGORY_TEXT = {
  green: { icon: '🟢', requirement: 'Can merge without human approval once CI and the agent review pass.' },
  amber: { icon: '🟡', requirement: 'Needs approval from one human code owner.' },
  red: {
    icon: '🔴',
    requirement: 'Needs expert review. A human owns the design decision and is accountable for the release.',
  },
};

const LEVEL_ICONS = { low: '🟢', medium: '🟡', high: '🔴' };

/** Characters of diff that the agent reads, about 100k tokens */
export const DIFF_BUDGET = 400_000;

/** The pull request files endpoint lists at most this many files */
const MAX_LISTED_FILES = 3000;

/** Lockfiles go last in the diff, so that their size does not crowd out the code */
const LOCKFILE = /(?:^|\/)(?:yarn\.lock|package-lock\.json)$/u;

// A blank line, or a line that only holds a comment: a line comment, a block
// comment that ends on the line, a JSDoc ` * …` line, or the end of a block
// comment, with no code after the comment
const COMMENT_LINE = /^\s*(?:$|\/\/|\*\/\s*$|(?:\/\*|\*(?!\/))(?:(?!\*\/).)*(?:\*\/\s*)?$)/u;

const MAX_LISTED_FINDING_FILES = 10;

/** Characters of each model text in the comment */
const MAX_TEXT = 1000;

/** The probability that a green suggestion needs, when the agent reports one */
export const GREEN_PROBABILITY = 0.8;

export function maxCategory(a, b) {
  return CATEGORIES.indexOf(a) >= CATEGORIES.indexOf(b) ? a : b;
}

function percent(value) {
  return `${Math.round(value * 100)}%`;
}

/** The added and removed lines of a unified diff patch, without the hunk headers */
export function changedLines(patch = '') {
  const added = [];
  const removed = [];
  for (const line of patch.split('\n')) {
    if (line.startsWith('+')) {
      added.push(line.slice(1));
    } else if (line.startsWith('-')) {
      removed.push(line.slice(1));
    }
  }
  return { added, removed };
}

// `matchesGlob` has no option to let wildcards match names that start with a
// dot, such as `.lib` in `src/.lib/x.js`. A leading dot is swapped for a
// character that globs treat as plain, in the path and in the glob alike.
function hideDots(value) {
  return value.replace(/(^|\/)\./gu, '$1\u2024');
}

function matchesGlobs(path, globs) {
  return globs.some((glob) => matchesGlob(hideDots(path), hideDots(glob)));
}

function paths(file) {
  return file.previous_filename ? [file.filename, file.previous_filename] : [file.filename];
}

function matchesAny(file, globs) {
  return paths(file).some((path) => matchesGlobs(path, globs));
}

/** Whether the file changes more than comments and blank lines. A file without a patch counts as code. */
function changesCode(file) {
  if (!file.patch) {
    return true;
  }
  const { added, removed } = changedLines(file.patch);
  return [...added, ...removed].some((line) => !COMMENT_LINE.test(line));
}

function isShipped(path, policy) {
  return matchesGlobs(path, policy.contentPaths) && !matchesGlobs(path, policy.testPaths);
}

/**
 * The rules of the policy of the `repository` (`owner/name`) that match a pull
 * request, each with the minimum category it sets and the files that made it
 * match.
 */
export function evaluatePolicy({ repository, title, body, authorTrusted, headTrusted, files }) {
  const policy = policyFor(repository);
  const findings = [];
  const shipped = files.filter((file) => isShipped(file.filename, policy));

  for (const rule of policy.pathRules) {
    const matched = files.filter((file) => matchesAny(file, rule.paths) && (!rule.codeOnly || changesCode(file)));
    if (matched.length > 0) {
      findings.push({ category: rule.category, reason: rule.reason, files: matched.map((file) => file.filename) });
    }
  }

  for (const rule of CONTENT_RULES) {
    const matched = shipped.filter((file) => changedLines(file.patch).added.some((line) => rule.pattern.test(line)));
    if (matched.length > 0) {
      findings.push({ category: rule.category, reason: rule.reason, files: matched.map((file) => file.filename) });
    }
  }

  // The content rules only see the changed lines, not the rest of a file that moves in
  const movedIn = shipped.filter((file) => file.previous_filename && !isShipped(file.previous_filename, policy));
  if (movedIn.length > 0) {
    findings.push({
      category: 'amber',
      reason: 'moves files into shipped code, so the rules did not check all of their content',
      files: movedIn.map((file) => file.filename),
    });
  }

  if (BREAKING_TITLE.test(title)) {
    findings.push({ category: 'red', reason: 'breaking change (`!` in the title)', files: [] });
  }
  if (BREAKING_BODY.test(body ?? '')) {
    findings.push({ category: 'red', reason: 'breaking change (`BREAKING CHANGE:` in the description)', files: [] });
  }

  if (!authorTrusted) {
    findings.push({ category: 'amber', reason: 'the author is not a member of the organization', files: [] });
  }
  if (!headTrusted) {
    findings.push({
      category: 'amber',
      reason: 'the head branch is in a fork that the author does not own',
      files: [],
    });
  }

  return findings;
}

export function isTrustedAuthor({ authorAssociation, isMember }) {
  return isMember || TRUSTED_ASSOCIATIONS.includes(authorAssociation);
}

/**
 * Whether the head branch is in the base repository or in a fork of the
 * author. Others who can push to the head could change the pull request
 * after a trusted author opened it.
 */
export function isTrustedHead(pr) {
  return pr.head.repo?.full_name === pr.base.repo.full_name || pr.head.repo?.owner.login === pr.user.login;
}

function diffHeader(file) {
  const before = file.status === 'added' ? '/dev/null' : `a/${file.previous_filename ?? file.filename}`;
  const after = file.status === 'removed' ? '/dev/null' : `b/${file.filename}`;
  return `diff --git a/${file.previous_filename ?? file.filename} b/${file.filename}\n--- ${before}\n+++ ${after}`;
}

/**
 * The unified diff that the agent reads, within the character budget. `omitted`
 * lists the changed files whose diff is not in `text`, because the API sent no
 * patch for them (binary files, files that git treats as binary, very large
 * diffs) or the budget ran out. Deleted files, pure renames and the
 * screenshots of the `repository` do not count as omitted.
 */
export function buildDiff(files, { repository, budget = DIFF_BUDGET } = {}) {
  const { screenshotPaths } = policyFor(repository);
  const ordered = [
    ...files.filter((file) => !LOCKFILE.test(file.filename)),
    ...files.filter((file) => LOCKFILE.test(file.filename)),
  ];
  const parts = [];
  const omitted = [];
  let size = 0;

  for (const file of ordered) {
    if (!file.patch) {
      const seen =
        file.status === 'removed' ||
        (file.status === 'renamed' && file.changes === 0) ||
        matchesGlobs(file.filename, screenshotPaths);
      if (!seen) {
        omitted.push(file.filename);
      }
      continue;
    }
    const part = `${diffHeader(file)}\n${file.patch}`;
    if (size + part.length > budget) {
      omitted.push(file.filename);
      continue;
    }
    parts.push(part);
    size += part.length;
  }

  if (files.length >= MAX_LISTED_FILES) {
    omitted.push(`files after the first ${MAX_LISTED_FILES}`);
  }

  return { text: parts.join('\n'), omitted };
}

/**
 * The final category of a pull request: the highest of the agent's suggestion
 * and the minimum categories that the policy findings set. Without an
 * assessment, or when the agent did not see the whole diff, the pull request
 * needs at least one human approval.
 */
export function classify({ policyFindings, omitted, assessment }) {
  const findings = [...policyFindings];

  if (omitted.length > 0) {
    findings.push({ category: 'amber', reason: 'the agent did not see the whole diff', files: omitted });
  }

  if (!assessment) {
    findings.push({ category: 'amber', reason: 'the agent assessment is not available', files: [] });
  } else if (assessment.category === 'green' && DIMENSIONS.some(({ key }) => assessment[key].level !== 'low')) {
    findings.push({
      category: 'amber',
      reason: 'the agent suggested green but rated a dimension above low',
      files: [],
    });
  } else if (assessment.category === 'green' && assessment.probabilities?.green < GREEN_PROBABILITY) {
    findings.push({
      category: 'amber',
      reason: `the agent gives green a probability of only ${percent(assessment.probabilities.green)}`,
      files: [],
    });
  }

  const category = findings.map((finding) => finding.category).reduce(maxCategory, assessment?.category ?? 'green');
  return { category, findings };
}

/**
 * Model output and file names come from the pull request, so keep them from
 * pinging users, breaking the table or making the comment too long
 */
function text(value) {
  const line = String(value)
    .replace(/\s*\n\s*/gu, ' ')
    .replace(/@(?=[\w-])/gu, '@\u200b')
    .trim();
  return line.length > MAX_TEXT ? `${line.slice(0, MAX_TEXT)}\u2026` : line;
}

function cell(value) {
  return text(value).replace(/\|/gu, '\\|');
}

/** Probabilities such as `low 53%, medium 40%, high 7%`, in the order the agent gave them */
function percentages(probabilities = {}) {
  return Object.entries(probabilities)
    .map(([value, probability]) => `${cell(value)} ${percent(probability)}`)
    .join(', ');
}

function code(value) {
  return `\`${text(value).replace(/`/gu, "'")}\``;
}

function renderFinding({ category, reason, files }) {
  const lines = [`- ${CATEGORY_TEXT[category].icon} **${category}**: ${reason}`];
  for (const file of files.slice(0, MAX_LISTED_FINDING_FILES)) {
    lines.push(`  - ${code(file)}`);
  }
  if (files.length > MAX_LISTED_FINDING_FILES) {
    lines.push(`  - and ${files.length - MAX_LISTED_FINDING_FILES} more`);
  }
  return lines;
}

/** The PR comment that explains the label. Starts with `marker`, which finds the comment again. */
export function renderComment({ marker, result, assessment, headSha, runUrl }) {
  const { category, findings } = result;
  const lines = [
    marker,
    `### ${CATEGORY_TEXT[category].icon} PR risk: ${category}`,
    '',
    CATEGORY_TEXT[category].requirement,
    '',
  ];

  if (assessment) {
    const agent = `The agent (${code(assessment.model)})`;
    const likely = assessment.probabilities ? ` (${percentages(assessment.probabilities)})` : '';
    const raised = category !== assessment.category ? ` The rules below raised it to **${category}**.` : '';
    const summary = assessment.summary === undefined ? '' : ` ${text(assessment.summary)}`;
    lines.push(`${agent} suggested **${assessment.category}**${likely}.${raised}${summary}`, '');

    // Claude explains each rating, the Decisions API gives the probability of each level
    const explained = DIMENSIONS.every(({ key }) => assessment[key].reason !== undefined);
    lines.push(`| Dimension | Risk | ${explained ? 'Reason' : 'Probabilities'} |`, '| --- | --- | --- |');
    for (const { key, title } of DIMENSIONS) {
      const { level, reason, probabilities } = assessment[key];
      const note = explained ? cell(reason) : percentages(probabilities);
      lines.push(`| ${title} | ${LEVEL_ICONS[level]} ${level} | ${note} |`);
    }
    lines.push('');
    if (assessment.rationale !== undefined) {
      lines.push(`> ${text(assessment.rationale)}`, '');
    }
  } else {
    lines.push(`The agent assessment failed, see the [workflow run](${runUrl}).`, '');
  }

  if (findings.length > 0) {
    lines.push('**Minimum category rules**', '');
    for (const finding of [...findings].sort(
      (a, b) => CATEGORIES.indexOf(b.category) - CATEGORIES.indexOf(a.category),
    )) {
      lines.push(...renderFinding(finding));
    }
    lines.push('');
  }

  lines.push(
    `<sub>Classified at ${headSha.slice(0, 10)} by the [PR risk workflow](${runUrl}). Every push classifies the pull request again.</sub>`,
    '',
  );
  return lines.join('\n');
}
