/**
 * Classifies a pull request from the command line the way the PR risk workflow
 * does, without changing the pull request. Prints the comment that the
 * workflow would post and the label that it would set. Pull requests of any
 * repository work: those without a part of their own in policy.js get the
 * generic rules, and the agent learns about the repository from GitHub.
 *
 *   node .github/pr-risk/cli.js [--no-cache] https://github.com/vaadin/flow/pull/26839
 *
 * Reads GitHub with GH_TOKEN or GITHUB_TOKEN when set; public repositories
 * also work without, at a lower rate limit. GitHub answers slowly, so its
 * answers are cached for an hour in node_modules/.cache/pr-risk; a note says
 * when cached data was used, and --no-cache reads GitHub again. The agent's
 * assessment is never cached. The OpenAI Decisions API assesses
 * the pull request when OPENAI_API_KEY is set, else Claude when
 * ANTHROPIC_API_KEY is set, else a model on OpenRouter when OPENROUTER_API_KEY
 * is set (OPENROUTER_MODEL picks it). Each needs the SDKs (`npm ci` in this
 * directory). Without a key, the label comes from the policy alone, as in a
 * workflow run whose assessment failed.
 */
import fs from 'node:fs/promises';
import { providerPackage } from './assess.js';
import { LABELS, run } from './main.js';

const USAGE = 'Usage: node .github/pr-risk/cli.js [--no-cache] https://github.com/<owner>/<repo>/pull/<number>';

const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN;

const CACHE_DIR = new URL('node_modules/.cache/pr-risk/', import.meta.url);
const CACHE_TTL_MS = 60 * 60 * 1000;

/** Whether to answer from the cache, and the age of the oldest cached answer used */
const cache = { use: true, oldest: 0 };

function parsePullRequest(arg = '') {
  const match = /^https:\/\/github\.com\/([\w.-]+)\/([\w.-]+)\/pull\/(\d+)/u.exec(arg);
  return match && { owner: match[1], repo: match[2], number: Number(match[3]) };
}

async function readCache(file) {
  try {
    const entry = JSON.parse(await fs.readFile(file, 'utf-8'));
    const age = Date.now() - entry.fetchedAt;
    return age < CACHE_TTL_MS ? { ...entry, age } : null;
  } catch {
    // Not cached yet, or the cache file is broken: read GitHub
    return null;
  }
}

/** The status and JSON body of a GET request to the GitHub API, from the cache when fresh enough */
async function request(path) {
  const file = new URL(`${path.replace(/[^\w.-]+/gu, '_')}.json`, CACHE_DIR);
  const cached = cache.use && (await readCache(file));
  if (cached) {
    cache.oldest = Math.max(cache.oldest, cached.age);
    return cached;
  }

  const response = await fetch(`https://api.github.com/${path}`, {
    headers: { accept: 'application/vnd.github+json', ...(token && { authorization: `Bearer ${token}` }) },
  });
  if (!response.ok && response.status !== 404) {
    throw new Error(`GET ${path} failed: ${response.status} ${response.statusText}`);
  }
  const entry = {
    fetchedAt: Date.now(),
    status: response.status,
    body: response.status === 204 ? null : await response.json(),
  };
  await fs.mkdir(CACHE_DIR, { recursive: true });
  await fs.writeFile(file, JSON.stringify(entry));
  return entry;
}

async function listFiles({ owner, repo, pull_number }) {
  const files = [];
  // The endpoint lists at most 3000 files, 100 per page
  for (let page = 1; page <= 30; page++) {
    const { body: batch } = await request(
      `repos/${owner}/${repo}/pulls/${pull_number}/files?per_page=100&page=${page}`,
    );
    files.push(...batch);
    if (batch.length < 100) {
      break;
    }
  }
  return files;
}

async function isMember(owner, login) {
  // 204 for members; 404, or a redirect to the public members, otherwise
  const { status } = await request(`orgs/${owner}/members/${login}`);
  return status === 204;
}

/**
 * Reads from the GitHub API like the workflow's client, and records the writes
 * instead of making them. Keeps the pull request as GitHub returned it in
 * `seen.pr`.
 */
function createDryRunGithub(writes, seen) {
  const record = (name) => (params) => {
    writes.push({ name, params });
    return Promise.resolve({ data: {} });
  };
  return {
    rest: {
      pulls: {
        get: async ({ owner, repo, pull_number }) => {
          const { status, body } = await request(`repos/${owner}/${repo}/pulls/${pull_number}`);
          if (status === 404) {
            throw new Error(`${owner}/${repo}#${pull_number} not found`);
          }
          seen.pr = body;
          // The workflow skips closed and draft pull requests, the command line classifies them too
          return { data: { ...seen.pr, state: 'open', draft: false } };
        },
        listFiles,
      },
      checks: {
        create: record('createCheck'),
      },
      issues: {
        // An existing comment only decides between creating and updating it
        listComments: () => [],
        createLabel: record('createLabel'),
        addLabels: record('addLabels'),
        removeLabel: record('removeLabel'),
        createComment: record('createComment'),
        updateComment: record('updateComment'),
      },
    },
    paginate: (endpoint, params) => Promise.resolve(endpoint(params)),
  };
}

function createCore(outputs, notes) {
  return {
    summary: { addRaw: () => ({ write: () => Promise.resolve() }) },
    setOutput: (name, value) => {
      outputs[name] = value;
    },
    notice: (message) => notes.push(message),
    setFailed: (message) => notes.push(message),
  };
}

async function main() {
  const args = process.argv.slice(2);
  cache.use = !args.includes('--no-cache');
  const pullRequest = parsePullRequest(args.find((arg) => !arg.startsWith('--')));
  if (!pullRequest) {
    console.error(USAGE);
    process.exitCode = 2;
    return;
  }

  // Loading the SDK takes a moment, so it loads while GitHub is read
  const sdk = providerPackage(process.env);
  const sdkLoaded = sdk
    ? import(sdk).then(
        () => true,
        () => false,
      )
    : Promise.resolve(true);

  const { owner, repo, number } = pullRequest;
  const writes = [];
  const seen = {};
  const outputs = {};
  const notes = [];
  try {
    await run({
      github: createDryRunGithub(writes, seen),
      context: {
        repo: { owner, repo },
        payload: { pull_request: { number } },
        serverUrl: 'https://github.com',
        runId: 'local',
      },
      core: createCore(outputs, notes),
      isMember: (login) => isMember(owner, login),
    });
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
    return;
  }
  if (!(await sdkLoaded)) {
    console.error(`${sdk} is missing. Run \`npm ci\` in .github/pr-risk first.`);
    process.exitCode = 1;
    return;
  }

  const comment = writes.find(({ name }) => name === 'createComment');
  if (comment) {
    console.log(comment.params.body);
  }
  if (seen.pr.state !== 'open' || seen.pr.draft) {
    const state = seen.pr.draft ? 'a draft' : seen.pr.state;
    notes.push(`#${number} is ${state}, so the workflow would not classify it now`);
  }
  if (cache.oldest > 0) {
    const minutes = Math.max(1, Math.round(cache.oldest / 60_000));
    notes.push(`GitHub data from the cache, up to ${minutes} min old. Pass --no-cache to read GitHub again.`);
  }
  for (const note of notes) {
    console.error(`Note: ${note}`);
  }
  if (outputs.category && process.env.PR_RISK_TRIAL === 'true') {
    console.log(`Category: ${outputs.category} (on trial, the workflow sets no label)`);
  } else if (outputs.category) {
    console.log(`Label: ${LABELS[outputs.category].name}`);
  }
}

await main();
