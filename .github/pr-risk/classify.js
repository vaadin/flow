/**
 * Pure functions of the PR risk workflow: evaluate the policy, build the diff
 * that the agent reads, combine the policy with the agent's assessment into the
 * final level, and render the PR comment.
 */
import { matchesGlob } from 'node:path';
import {
  BREAKING_BODY,
  BREAKING_TITLE,
  CONTENT_RULES,
  ISSUE_LABEL_RULES,
  PATCH_BUMPS,
  PATH_RULES,
  SHIPPED_PATHS,
  TEST_PATHS,
  TRUSTED_ASSOCIATIONS,
} from './policy.js';

/** From lowest to highest risk */
export const LEVELS = ['low', 'medium', 'high'];

export const DIMENSIONS = [
  { key: 'impact', title: 'Impact' },
  { key: 'blast_radius', title: 'Blast radius' },
  { key: 'uncertainty', title: 'Uncertainty' },
  { key: 'reversibility', title: 'Reversibility' },
];

const ICONS = { low: '🟢', medium: '🟡', high: '🔴' };

const REQUIREMENTS = {
  low: 'Could merge without human approval once CI and the agent review pass.',
  medium: 'Needs approval from one human code owner.',
  high: 'Needs expert review. A human owns the design decision and is accountable for the release.',
};

/** Characters of diff that the agent reads, about 100k tokens */
export const DIFF_BUDGET = 400_000;

/** The pull request files endpoint lists at most this many files */
const MAX_LISTED_FILES = 3000;

/** Lockfiles go last in the diff, so that their size does not crowd out the code */
const LOCKFILE = /(?:^|\/)(?:pnpm-lock\.yaml|package-lock\.json)$/u;

const JAVA_FILE = /\.java$/u;

/** A Java string literal on one line */
const JAVA_STRING = /"(?:[^"\\]|\\.)*"/gu;

/** The blob of an empty file, which the API sends without a patch */
const EMPTY_BLOB = 'e69de29bb2d1d6434b8b29ae775ad8c2e48c5391';

// A blank line, or a line that only holds a comment: a line comment, a block
// comment that ends on the line, a JSDoc ` * …` line, or the end of a block
// comment, with no code after the comment
const COMMENT_LINE = /^\s*(?:$|\/\/|\*\/\s*$|(?:\/\*|\*(?!\/))(?:(?!\*\/).)*(?:\*\/\s*)?$)/u;

const MAX_LISTED_FINDING_FILES = 10;

/** Characters of each model text in the comment */
const MAX_TEXT = 1000;

export function maxLevel(a, b) {
  return LEVELS.indexOf(a) >= LEVELS.indexOf(b) ? a : b;
}

/** The added and removed lines of a unified diff patch, without the hunk headers */
export function changedLines(patch) {
  const added = [];
  const removed = [];
  for (const line of (patch ?? '').split('\n')) {
    if (line.startsWith('+')) {
      added.push(line.slice(1));
    } else if (line.startsWith('-')) {
      removed.push(line.slice(1));
    }
  }
  return { added, removed };
}

// `matchesGlob` has no option to let wildcards match names that start with a
// dot. A leading dot is swapped for a character that globs treat as plain, in
// the path and in the glob alike.
function hideDots(value) {
  return value.replace(/(^|\/)\./gu, '$1․');
}

function matchesGlobs(path, globs) {
  return globs.some((glob) => matchesGlob(hideDots(path), hideDots(glob)));
}

function paths(file) {
  return file.previous_filename ? [file.filename, file.previous_filename] : [file.filename];
}

function matchesAny(file, globs = []) {
  return paths(file).some((path) => matchesGlobs(path, globs));
}

function matchesRule(file, rule) {
  return matchesAny(file, rule.paths) && !paths(file).every((path) => matchesGlobs(path, rule.exceptPaths ?? []));
}

/** Whether an added `line` of `file` matches a content rule, see CONTENT_RULES */
function matchesContent(rule, file, line) {
  let text = line;
  if (JAVA_FILE.test(file.filename)) {
    if (COMMENT_LINE.test(line)) {
      return false;
    }
    if (rule.javaStringsOnly) {
      text = (line.match(JAVA_STRING) ?? []).join(' ');
    }
  }
  return rule.pattern.test(text) && !rule.except?.test(line);
}

/** Whether the file changes more than comments and blank lines. A file without a patch counts as code. */
function changesCode(file) {
  if (!file.patch) {
    return true;
  }
  const { added, removed } = changedLines(file.patch);
  return [...added, ...removed].some((line) => !COMMENT_LINE.test(line));
}

function isShipped(path) {
  return matchesGlobs(path, SHIPPED_PATHS) && !matchesGlobs(path, TEST_PATHS);
}

/**
 * Whether a pull request is a PATCH_BUMPS update: an allowed author, a title
 * that bumps x.y.z to x.y.(z+1) or later, and a diff whose every change swaps
 * the old version for the new one.
 */
function isPatchBump({ author, title, files }) {
  const match = PATCH_BUMPS.authors.includes(author) && PATCH_BUMPS.title.exec(title);
  if (!match) {
    return false;
  }
  const [from, to] = [match.slice(1, 4).join('.'), match.slice(4, 7).join('.')];
  const [major, minor, patch, toMajor, toMinor, toPatch] = match.slice(1, 7).map(Number);
  if (major !== toMajor || minor !== toMinor || toPatch <= patch) {
    return false;
  }
  return (
    files.length > 0 &&
    files.every((file) => {
      const { added, removed } = changedLines(file.patch);
      return (
        file.status === 'modified' &&
        !file.previous_filename &&
        removed.length > 0 &&
        added.length === removed.length &&
        removed.every((line, index) => line.includes(from) && line.replaceAll(from, to) === added[index])
      );
    })
  );
}

/** Splits the files that match a path rule by the level they get: the rule's own, `removed` or `checksOnly` */
function* pathFindings(rule, files) {
  const matched = files.filter((file) => matchesRule(file, rule) && (!rule.codeOnly || changesCode(file)));
  const removed = rule.removed
    ? matched.filter((file) => file.status === 'removed' && matchesAny(file, rule.removed.paths))
    : [];
  const checks = rule.checksOnly;
  const checksOnly = checks
    ? matched.filter(
        (file) =>
          !removed.includes(file) &&
          file.patch &&
          matchesAny(file, checks.paths) &&
          !matchesAny(file, checks.exceptPaths) &&
          !Object.values(changedLines(file.patch))
            .flat()
            .some((line) => checks.sensitive.test(line)),
      )
    : [];
  const changed = matched.filter((file) => !removed.includes(file) && !checksOnly.includes(file));
  for (const [source, group] of [
    [rule, changed],
    [rule.removed, removed],
    [checks, checksOnly],
  ]) {
    if (group.length > 0) {
      yield { level: source.level, reason: source.reason, files: group.map((file) => file.filename) };
    }
  }
}

/**
 * The rules of the policy that match a pull request, each with the minimum
 * level it sets and the files that made it match. `author` is the login of the
 * author of the pull request.
 */
export function evaluatePolicy({ author, title, body, authorTrusted, headTrusted, files, linkedIssues = [] }) {
  const findings = [];
  const shipped = files.filter((file) => isShipped(file.filename));

  const patchBump = isPatchBump({ author, title, files });
  if (patchBump) {
    findings.push({ level: 'low', reason: PATCH_BUMPS.reason, files: files.map((file) => file.filename) });
  }

  for (const rule of PATH_RULES) {
    if (!(patchBump && rule.exceptPatchBumps)) {
      findings.push(...pathFindings(rule, files));
    }
  }

  for (const rule of CONTENT_RULES) {
    const matched = shipped.filter(
      (file) =>
        !matchesAny(file, rule.exceptPaths) &&
        changedLines(file.patch).added.some((line) => matchesContent(rule, file, line)),
    );
    if (matched.length > 0) {
      findings.push({ level: rule.level, reason: rule.reason, files: matched.map((file) => file.filename) });
    }
  }

  // The content rules only see the changed lines, not the rest of a file that moves in
  const movedIn = shipped.filter((file) => file.previous_filename && !isShipped(file.previous_filename));
  if (movedIn.length > 0) {
    findings.push({
      level: 'medium',
      reason: 'moves files into shipped code, so the rules did not check all of their content',
      files: movedIn.map((file) => file.filename),
    });
  }

  for (const rule of ISSUE_LABEL_RULES) {
    const matched = linkedIssues.filter((issue) => issue.labels.includes(rule.label)).map((issue) => issue.ref);
    if (matched.length > 0) {
      findings.push({ level: rule.level, reason: rule.reason, files: matched });
    }
  }

  if (BREAKING_TITLE.test(title)) {
    findings.push({ level: 'high', reason: 'breaking change (`!` in the title)', files: [] });
  }
  if (BREAKING_BODY.test(body ?? '')) {
    findings.push({ level: 'high', reason: 'breaking change (`BREAKING CHANGE:` in the description)', files: [] });
  }
  if (!authorTrusted) {
    findings.push({ level: 'medium', reason: 'the author is not a member of the organization', files: [] });
  }
  if (!headTrusted) {
    findings.push({ level: 'medium', reason: 'the head branch is in a fork that the author does not own', files: [] });
  }
  return findings;
}

/** Linked issues that the workflow reads at most */
const MAX_LINKED_ISSUES = 5;

const ISSUE_REF = String.raw`(?:https:\/\/github\.com\/([\w.-]+)\/([\w.-]+)\/issues\/(\d+)|(?:([\w.-]+)\/([\w.-]+))?#(\d+))`;

/** A reference that a closing keyword or `Part of` links, as in the PR template */
const LINKED_ISSUE = new RegExp(
  String.raw`\b(?:fix(?:es|ed)?|close[sd]?|resolve[sd]?|part of)\b:?\s+${ISSUE_REF}`,
  'giu',
);

/**
 * The issues that the description links with `Fixes`, `Closes`, `Resolves` or
 * `Part of`, as `{ owner, repo, number }`, at most MAX_LINKED_ISSUES. A bare
 * `#123` is in the base repository `{ owner, repo }`. Only issues of the same
 * owner count, so a description cannot make the workflow read other accounts.
 */
export function linkedIssueRefs(body, { owner, repo }) {
  const refs = [];
  for (const match of (body ?? '').matchAll(LINKED_ISSUE)) {
    const ref = {
      owner: match[1] ?? match[4] ?? owner,
      repo: match[2] ?? match[5] ?? repo,
      number: Number(match[3] ?? match[6]),
    };
    const known = refs.some(
      (other) => other.owner === ref.owner && other.repo === ref.repo && other.number === ref.number,
    );
    if (ref.owner.toLowerCase() === owner.toLowerCase() && !known) {
      refs.push(ref);
    }
  }
  return refs.slice(0, MAX_LINKED_ISSUES);
}

export function isTrustedAuthor({ authorAssociation, isMember, isBot = false }) {
  return isMember || isBot || TRUSTED_ASSOCIATIONS.includes(authorAssociation);
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
 * patch for them (binary files, very large diffs) or the budget ran out.
 * Deleted files, pure renames and empty files do not count as omitted.
 */
export function buildDiff(files, budget = DIFF_BUDGET) {
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
        file.status === 'removed' || (file.status === 'renamed' && file.changes === 0) || file.sha === EMPTY_BLOB;
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

/** The agent's suggestion as it counts: high needs at least one dimension rated high, else it counts as medium */
export function agentLevel(assessment) {
  if (assessment.level === 'high' && DIMENSIONS.every(({ key }) => assessment[key].level !== 'high')) {
    return 'medium';
  }
  return assessment.level;
}

/**
 * The final level of a pull request: the highest of the agent's suggestion
 * (see agentLevel) and the minimum levels that the policy findings set.
 * Without an assessment, or when the agent did not see the whole diff, the
 * pull request is at least medium. A `skipped` assessment, for an author
 * outside the organization, is not reported as a failure.
 */
export function classify({ policyFindings, omitted, assessment, skipped = false }) {
  const findings = [...policyFindings];
  if (omitted.length > 0) {
    findings.push({ level: 'medium', reason: 'the agent did not see the whole diff', files: omitted });
  }
  if (!assessment && !skipped) {
    findings.push({ level: 'medium', reason: 'the agent assessment is not available', files: [] });
  }
  if (assessment?.level === 'low' && DIMENSIONS.some(({ key }) => assessment[key].level !== 'low')) {
    findings.push({ level: 'medium', reason: 'the agent suggested low but rated a dimension above low', files: [] });
  }
  const floor = assessment ? agentLevel(assessment) : 'medium';
  return { level: findings.map((finding) => finding.level).reduce(maxLevel, floor), findings };
}

/**
 * Model output and file names come from the pull request, so keep them from
 * pinging users, breaking the table or making the comment too long
 */
function text(value) {
  const line = String(value)
    .replace(/\s*\n\s*/gu, ' ')
    .replace(/@(?=[\w-])/gu, '@​')
    .trim();
  return line.length > MAX_TEXT ? `${line.slice(0, MAX_TEXT)}…` : line;
}

/**
 * Model text, which a pull request can steer, as plain text: no links, images
 * or HTML, so that the comment cannot carry a link that seems to come from the
 * workflow
 */
function prose(value) {
  return text(value)
    .replace(/[[\]<>]/gu, '\\$&')
    .replace(/:\/\//gu, ':/​/')
    .replace(/\bwww\./giu, 'www​.');
}

function code(value) {
  return `\`${text(value).replace(/`/gu, "'")}\``;
}

function renderFinding({ level, reason, files }) {
  const lines = [`- ${ICONS[level]} **${level}**: ${reason}`];
  for (const file of files.slice(0, MAX_LISTED_FINDING_FILES)) {
    lines.push(`  - ${code(file)}`);
  }
  if (files.length > MAX_LISTED_FINDING_FILES) {
    lines.push(`  - and ${files.length - MAX_LISTED_FINDING_FILES} more`);
  }
  return lines;
}

/** The PR comment that explains the level. Starts with `marker`, which finds the comment again. */
export function renderComment({ marker, result, assessment, skipped = false, headSha, runUrl }) {
  const { level, findings } = result;
  const lines = [
    marker,
    `### 🧪 Risk evaluation (trial): ${ICONS[level]} ${level} risk`,
    '',
    '> This workflow is on trial. It only posts this comment, and nothing in the review or merge process depends on it. Is the rating wrong? Say so in a reply.',
    '',
    `If it were live: ${REQUIREMENTS[level]}`,
    '',
  ];

  if (assessment) {
    const counted = agentLevel(assessment);
    const capped = counted !== assessment.level ? ` It rated no dimension high, so it counts as **${counted}**.` : '';
    const raised = level !== counted ? ` The rules below raised it to **${level}**.` : '';
    lines.push(
      `The agent (${code(assessment.model)}) suggested **${assessment.level}**.${capped}${raised} ${prose(assessment.summary)}`,
      '',
      '| Dimension | Risk | Reason |',
      '| --- | --- | --- |',
      ...DIMENSIONS.map(({ key, title }) => {
        const rating = assessment[key];
        return `| ${title} | ${ICONS[rating.level]} ${rating.level} | ${prose(rating.reason).replace(/\|/gu, '\\|')} |`;
      }),
      '',
      `> ${prose(assessment.rationale)}`,
      '',
    );
  } else if (skipped) {
    lines.push('The agent does not assess pull requests from authors outside the organization, so the rules below decide.', '');
  } else {
    lines.push(`The agent assessment failed, see the [workflow run](${runUrl}).`, '');
  }

  if (findings.length > 0) {
    lines.push('**Minimum risk rules**', '');
    for (const finding of [...findings].sort((a, b) => LEVELS.indexOf(b.level) - LEVELS.indexOf(a.level))) {
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
