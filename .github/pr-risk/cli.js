/**
 * Classifies a Flow pull request from the command line the way the PR risk
 * workflow does, without changing the pull request, and prints the comment
 * that the workflow would post.
 *
 *   node .github/pr-risk/cli.js https://github.com/vaadin/flow/pull/26839
 *
 * Reads GitHub with GH_TOKEN or GITHUB_TOKEN when set, and asks Claude when
 * ANTHROPIC_API_KEY is set (run `npm ci` in this directory first). Without a
 * key, the level comes from the rules alone, as in a run whose assessment
 * failed.
 */
import { run } from './main.js';

const USAGE = 'Usage: node .github/pr-risk/cli.js https://github.com/vaadin/flow/pull/<number>';

const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN;

/** The status and JSON body of a GET request to the GitHub API */
async function request(path) {
  const response = await fetch(`https://api.github.com/${path}`, {
    headers: { accept: 'application/vnd.github+json', ...(token && { authorization: `Bearer ${token}` }) },
  });
  if (!response.ok && response.status !== 404) {
    throw new Error(`GET ${path} failed: ${response.status} ${response.statusText}`);
  }
  return { status: response.status, body: response.status === 204 ? null : await response.json() };
}

/** A GitHub client that reads like the workflow's and only records the comment */
function createDryRunGithub(written, seen) {
  const read = async (path) => {
    const { status, body } = await request(path);
    if (status === 404) {
      throw Object.assign(new Error(`${path} not found`), { status });
    }
    return { data: body };
  };
  const listFiles = async ({ owner, repo, pull_number }) => {
    const files = [];
    // The endpoint lists at most 3000 files, 100 per page
    for (let page = 1; page <= 30; page++) {
      const { data } = await read(`repos/${owner}/${repo}/pulls/${pull_number}/files?per_page=100&page=${page}`);
      files.push(...data);
      if (data.length < 100) {
        break;
      }
    }
    return files;
  };
  return {
    rest: {
      pulls: {
        get: async ({ owner, repo, pull_number }) => {
          seen.pr = (await read(`repos/${owner}/${repo}/pulls/${pull_number}`)).data;
          // The workflow skips closed and draft pull requests, the command line classifies them too
          return { data: { ...seen.pr, state: 'open', draft: false } };
        },
        listFiles,
      },
      issues: {
        get: ({ owner, repo, issue_number }) => read(`repos/${owner}/${repo}/issues/${issue_number}`),
        listComments: () => [],
        createComment: ({ body }) => {
          written.body = body;
        },
      },
    },
    paginate: (endpoint, params) => Promise.resolve(endpoint(params)),
  };
}

async function main() {
  const match = /^https:\/\/github\.com\/([\w.-]+)\/([\w.-]+)\/pull\/(\d+)/u.exec(process.argv[2] ?? '');
  if (!match) {
    console.error(USAGE);
    process.exitCode = 2;
    return;
  }
  const [, owner, repo, number] = match;
  const written = {};
  const seen = {};
  const notes = [];
  const outputs = {};
  try {
    await run({
      github: createDryRunGithub(written, seen),
      context: {
        repo: { owner, repo },
        payload: { pull_request: { number: Number(number) } },
        serverUrl: 'https://github.com',
        runId: 'local',
      },
      core: {
        summary: { addRaw: () => ({ write: () => Promise.resolve() }) },
        setOutput: (name, value) => {
          outputs[name] = value;
        },
        notice: (message) => notes.push(message),
        setFailed: (message) => notes.push(message),
      },
      // 204 for members; 404, or a redirect to the public members, otherwise
      isMember: async (login) => (await request(`orgs/${owner}/members/${login}`)).status === 204,
    });
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
    return;
  }

  console.log(written.body);
  if (seen.pr.state !== 'open' || seen.pr.draft) {
    notes.push(`#${number} is ${seen.pr.draft ? 'a draft' : seen.pr.state}, so the workflow would not classify it now`);
  }
  for (const note of notes) {
    console.error(`Note: ${note}`);
  }
  console.log(`Level: ${outputs.level}`);
}

await main();
