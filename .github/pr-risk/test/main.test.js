import assert from 'node:assert/strict';
import { beforeEach, describe, test } from 'node:test';
import { MARKER, run } from '../main.js';

const rating = (level) => ({ level, reason: `rated ${level}` });
const LOW = {
  summary: 'Fixes a typo.',
  impact: rating('low'),
  blast_radius: rating('low'),
  uncertainty: rating('low'),
  reversibility: rating('low'),
  level: 'low',
  rationale: 'A documentation fix.',
  model: 'claude-test',
};

const README = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];

/** A fake GitHub client that records the comments it writes */
function createGithub({ pr, files = README, comments = [], issues = {} }) {
  const calls = [];
  const record = (name) => (params) => {
    calls.push([name, params]);
    return Promise.resolve({ data: {} });
  };
  return {
    calls,
    rest: {
      pulls: { get: () => Promise.resolve({ data: pr }), listFiles: 'listFiles' },
      issues: {
        get: ({ owner, repo, issue_number }) => {
          const issue = issues[`${owner}/${repo}#${issue_number}`];
          return issue ? Promise.resolve({ data: issue }) : Promise.reject(Object.assign(new Error('Not Found')));
        },
        listComments: 'listComments',
        createComment: record('createComment'),
        updateComment: record('updateComment'),
      },
    },
    paginate: (endpoint) => Promise.resolve(endpoint === 'listFiles' ? files : comments),
  };
}

function createCore() {
  const core = {
    failed: null,
    outputs: {},
    notices: [],
    summary: { addRaw: () => ({ write: () => Promise.resolve() }) },
    setFailed: (message) => {
      core.failed = message;
    },
    setOutput: (name, value) => {
      core.outputs[name] = value;
    },
    notice: (message) => core.notices.push(message),
  };
  return core;
}

const context = {
  repo: { owner: 'vaadin', repo: 'flow' },
  payload: { pull_request: { number: 42 } },
  serverUrl: 'https://github.com',
  runId: 7,
};

const env = { AUTHOR_IS_MEMBER: 'true' };

describe('run', () => {
  let pr;

  beforeEach(() => {
    pr = {
      number: 42,
      state: 'open',
      draft: false,
      title: 'docs: fix typo',
      body: '',
      author_association: 'CONTRIBUTOR',
      user: { login: 'dev' },
      head: { sha: 'abcdef0123456789', repo: { full_name: 'vaadin/flow', owner: { login: 'vaadin' } } },
      base: { repo: { full_name: 'vaadin/flow' } },
    };
  });

  test('posts one comment with the level', async () => {
    const github = createGithub({ pr });
    const core = createCore();
    await run({ github, context, core, env, assess: () => Promise.resolve(LOW) });

    assert.deepEqual(
      github.calls.map(([name]) => name),
      ['createComment'],
    );
    const [, comment] = github.calls[0];
    assert.equal(comment.issue_number, 42);
    assert.ok(comment.body.startsWith(MARKER));
    assert.match(comment.body, /🟢 low risk/u);
    assert.equal(core.outputs.level, 'low');
    assert.equal(core.failed, null);
  });

  test('updates its own comment of an earlier run, not one of someone else', async () => {
    const comments = [
      { id: 1, user: { login: 'someone' }, body: MARKER },
      { id: 2, user: { login: 'github-actions[bot]' }, body: `${MARKER}\nold` },
    ];
    const github = createGithub({ pr, comments });
    await run({ github, context, core: createCore(), env, assess: () => Promise.resolve(LOW) });
    assert.deepEqual(
      github.calls.map(([name, params]) => [name, params.comment_id]),
      [['updateComment', 2]],
    );
  });

  test('skips draft and closed pull requests', async () => {
    for (const changed of [{ draft: true }, { state: 'closed' }]) {
      const github = createGithub({ pr: { ...pr, ...changed } });
      const core = createCore();
      await run({ github, context, core, env, assess: () => assert.fail('assessed') });
      assert.deepEqual(github.calls, []);
      assert.match(core.notices[0], /closed or a draft/u);
    }
  });

  test('rates by the rules and fails the run when the assessment fails', async () => {
    const github = createGithub({ pr });
    const core = createCore();
    await run({ github, context, core, env, assess: () => Promise.reject(new Error('overloaded')) });
    assert.equal(core.outputs.level, 'medium');
    assert.equal(core.failed, 'The agent assessment failed: overloaded');
    assert.match(github.calls[0][1].body, /The agent assessment failed, see the \[workflow run\]/u);
  });

  test('does not pay for an assessment of an author outside the organization', async () => {
    const github = createGithub({ pr });
    const core = createCore();
    const asked = [];
    await run({
      github,
      context,
      core,
      env,
      assess: () => assert.fail('assessed'),
      isMember: (login) => {
        asked.push(login);
        return Promise.resolve(false);
      },
    });
    assert.deepEqual(asked, ['dev']);
    assert.equal(core.outputs.level, 'medium');
    assert.equal(core.failed, null);
    assert.match(github.calls[0][1].body, /does not assess pull requests from authors outside the organization/u);
  });

  test('gives the agent the linked issues it can read, and skips pull requests', async () => {
    const issues = {
      'vaadin/flow#1': { title: 'Bug', state: 'open', labels: [{ name: 'Impact: High' }] },
      'vaadin/flow#2': { title: 'A pull request', state: 'open', labels: [], pull_request: {} },
    };
    const github = createGithub({ pr: { ...pr, body: 'Fixes #1, fixes #2, fixes #3' }, issues });
    let seen;
    await run({
      github,
      context,
      core: createCore(),
      env,
      assess: ({ linkedIssues }) => {
        seen = linkedIssues;
        return Promise.resolve(LOW);
      },
    });
    assert.deepEqual(seen, [{ ref: 'vaadin/flow#1', title: 'Bug', state: 'open', labels: ['Impact: High'] }]);
    assert.match(github.calls[0][1].body, /Impact: High/u);
  });
});
