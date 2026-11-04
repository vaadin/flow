import assert from 'node:assert/strict';
import { beforeEach, describe, test } from 'node:test';
import { MARKER, run } from '../main.js';

function rating(level) {
  return { level, reason: `rated ${level}` };
}

const GREEN = {
  summary: 'Fixes a typo.',
  impact: rating('low'),
  blast_radius: rating('low'),
  uncertainty: rating('low'),
  reversibility: rating('low'),
  category: 'green',
  rationale: 'A documentation fix.',
};

/** A fake GitHub client that records the calls that change the pull request */
function createGithub({ pr, files, comments = [] }) {
  const calls = [];
  const record = (name) => (params) => {
    calls.push([name, params]);
    return Promise.resolve({ data: {} });
  };
  const github = {
    calls,
    rest: {
      pulls: {
        get: () => Promise.resolve({ data: pr }),
        listFiles: 'listFiles',
      },
      checks: {
        create: record('createCheck'),
      },
      issues: {
        listComments: 'listComments',
        createLabel: record('createLabel'),
        addLabels: record('addLabels'),
        removeLabel: record('removeLabel'),
        createComment: record('createComment'),
        updateComment: record('updateComment'),
      },
    },
    paginate: (endpoint) => Promise.resolve(endpoint === 'listFiles' ? files : comments),
  };
  return github;
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
  repo: { owner: 'vaadin', repo: 'web-components' },
  payload: { pull_request: { number: 42 } },
  serverUrl: 'https://github.com',
  runId: 7,
};

const env = { ANTHROPIC_API_KEY: 'key', AUTHOR_IS_MEMBER: 'true' };

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
      head: { sha: 'abcdef0123456789', repo: { full_name: 'vaadin/web-components', owner: { login: 'vaadin' } } },
      base: { repo: { full_name: 'vaadin/web-components' } },
      labels: [{ name: 'risk: red' }, { name: 'a11y' }],
    };
  });

  test('replaces the risk label and creates the comment', async () => {
    const files = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];
    const github = createGithub({ pr, files });
    const core = createCore();
    await run({ github, context, core, env, assess: () => Promise.resolve(GREEN) });

    // The old label goes first, so that a run that stops halfway leaves no label
    assert.deepEqual(
      github.calls.map(([name]) => name),
      ['createCheck', 'removeLabel', 'createLabel', 'addLabels', 'createComment'],
    );
    assert.equal(github.calls[1][1].name, 'risk: red');
    assert.deepEqual(github.calls[3][1].labels, ['risk: green']);
    assert.ok(github.calls[4][1].body.startsWith(MARKER));
    assert.equal(core.outputs.category, 'green');
    assert.equal(core.failed, null);
  });

  test('updates the comment of an earlier run and keeps a label that is still right', async () => {
    pr.labels = [{ name: 'risk: red' }];
    const files = [{ filename: '.github/workflows/verify.yml', status: 'modified', changes: 2, patch: '@@\n-a\n+b' }];
    const comments = [
      { id: 1, user: { login: 'someone' }, body: MARKER },
      { id: 2, user: { login: 'github-actions[bot]' }, body: `${MARKER}\nold` },
    ];
    const github = createGithub({ pr, files, comments });
    await run({ github, context, core: createCore(), env, assess: () => Promise.resolve(GREEN) });

    assert.deepEqual(
      github.calls.map(([name]) => name),
      ['createCheck', 'updateComment'],
    );
    assert.equal(github.calls[1][1].comment_id, 2);
  });

  test('labels by the policy alone and fails the run when the assessment fails', async () => {
    const files = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];
    const github = createGithub({ pr, files });
    const core = createCore();
    await run({
      github,
      context,
      core,
      env,
      assess: () => Promise.reject(new Error('overloaded')),
    });

    assert.deepEqual(github.calls.find(([name]) => name === 'addLabels')[1].labels, ['risk: amber']);
    assert.equal(core.failed, 'The agent assessment failed: overloaded');
  });

  test('removes a green label before the assessment', async () => {
    pr.labels = [{ name: 'risk: green' }];
    const files = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];
    const github = createGithub({ pr, files });
    let callsBeforeAssessment;
    const assess = () => {
      callsBeforeAssessment = github.calls.map(([name, params]) => `${name} ${params.name}`);
      return Promise.resolve(GREEN);
    };
    await run({ github, context, core: createCore(), env, assess });

    assert.deepEqual(callsBeforeAssessment, ['removeLabel risk: green']);
    assert.deepEqual(github.calls.find(([name]) => name === 'addLabels')[1].labels, ['risk: green']);
  });

  test('finishes removing a green label before it sets the label again', async () => {
    pr.labels = [{ name: 'risk: green' }];
    const files = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];
    const github = createGithub({ pr, files });
    github.rest.issues.removeLabel = (params) => {
      github.calls.push(['removeLabel', params]);
      return new Promise((resolve) => {
        setTimeout(() => {
          github.calls.push(['removeLabel done', params]);
          resolve({ data: {} });
        }, 20);
      });
    };
    await run({ github, context, core: createCore(), env, assess: () => Promise.resolve(GREEN) });

    const labelCalls = github.calls.map(([name]) => name).filter((name) => /Label/u.test(name));
    assert.deepEqual(labelCalls, ['removeLabel', 'removeLabel done', 'createLabel', 'addLabels']);
  });

  test('asks whether the author is a member', async () => {
    const files = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];
    const asked = [];
    const isMember = (login) => {
      asked.push(login);
      return Promise.resolve(false);
    };
    const core = createCore();
    await run({
      github: createGithub({ pr, files }),
      context,
      core,
      env,
      assess: () => Promise.resolve(GREEN),
      isMember,
    });

    assert.deepEqual(asked, ['dev']);
    assert.equal(core.outputs.category, 'amber');
  });

  test('reports the category as a check run on the head commit', async () => {
    const files = [{ filename: 'README.md', status: 'modified', changes: 2, patch: '@@\n-teh\n+the' }];
    const green = createGithub({ pr, files });
    await run({ github: green, context, core: createCore(), env, assess: () => Promise.resolve(GREEN) });
    const [, check] = green.calls.find(([name]) => name === 'createCheck');
    assert.deepEqual(
      { ...check, output: check.output.title },
      {
        owner: 'vaadin',
        repo: 'web-components',
        name: 'PR risk',
        head_sha: 'abcdef0123456789',
        status: 'completed',
        conclusion: 'success',
        external_id: '7',
        details_url: 'https://github.com/vaadin/web-components/actions/runs/7',
        output: 'Green',
      },
    );
    assert.ok(check.output.summary.startsWith(MARKER));

    pr.title = 'feat!: remove the API';
    const red = createGithub({ pr, files });
    await run({ github: red, context, core: createCore(), env, assess: () => Promise.resolve(GREEN) });
    const [, redCheck] = red.calls.find(([name]) => name === 'createCheck');
    assert.equal(redCheck.conclusion, 'neutral');
    assert.equal(redCheck.output.title, 'Red');
  });

  test('skips draft pull requests', async () => {
    pr.draft = true;
    const github = createGithub({ pr, files: [] });
    const core = createCore();
    await run({ github, context, core, env, assess: () => Promise.resolve(GREEN) });

    assert.deepEqual(github.calls, []);
    assert.equal(core.notices.length, 1);
  });
});
