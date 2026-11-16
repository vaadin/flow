// Run with: node --test scripts/runMavenWithRetry.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const SCRIPT = path.join(__dirname, 'runMavenWithRetry.sh');

const TRANSFER_ERROR =
  '[ERROR] Failed to execute goal on project foo: Could not transfer artifact a:b:pom:1 from/to central: status code: 502';
const TRANSFER_WARNING = '[WARNING] Could not transfer metadata a:b:1-SNAPSHOT/maven-metadata.xml from/to snapshots';
const TEST_FAILURE =
  '[ERROR] Failed to execute goal org.apache.maven.plugins:maven-failsafe-plugin:3.5.3:verify (default) on project bar: There are test failures.';
const FLAKY_CLASS = '[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 2 s <<< FAILURE! -- in FooIT';
const OTHER_FAILURE = '[ERROR] Failed to execute goal on project foo: Compilation failure';

/**
 * Runs the script with a fake mvn that prints the given log and exits with
 * the given status on each attempt, repeating the last attempt when there
 * are more attempts than entries. sleep and pkill are stubbed so that a
 * retry neither waits nor stops other Java processes. env is added to the
 * script's environment, which otherwise sets no attempt limit or deadline.
 * When clock is given, date is stubbed to return that time and then 100
 * seconds more on each call, so every attempt takes 100 seconds and the
 * next one starts 100 seconds after it ended.
 */
function run(attempts, env = {}, clock) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mvn-retry-'));
  try {
    fs.writeFileSync(path.join(dir, 'attempts.json'), JSON.stringify(attempts));
    const bin = (name, body) => fs.writeFileSync(path.join(dir, name), `#!/usr/bin/env bash\n${body}\n`, { mode: 0o755 });
    bin(
      'mvn',
      `count=$(cat "${dir}/count" 2>/dev/null || echo 0)
echo $((count + 1)) > "${dir}/count"
exec node -e '
  const all = require(process.argv[1]);
  const { log, status } = all[Math.min(+process.argv[2], all.length - 1)];
  console.log(log);
  process.exitCode = status;
' "${dir}/attempts.json" "$count"`
    );
    if (clock) {
      bin(
        'date',
        `calls=$(cat "${dir}/calls" 2>/dev/null || echo 0)
echo $((calls + 1)) > "${dir}/calls"
echo $((${clock} + calls * 100))`
      );
    }
    bin('sleep', 'true');
    bin('pkill', 'true');
    const { MAVEN_MAX_ATTEMPTS, MAVEN_RETRY_DEADLINE, ...inherited } = process.env;
    const result = spawnSync(SCRIPT, [path.join(dir, 'mvn.out')], {
      env: { ...inherited, ...env, PATH: `${dir}:${process.env.PATH}` },
      encoding: 'utf8'
    });
    return { status: result.status, runs: +fs.readFileSync(path.join(dir, 'count'), 'utf8') };
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

test('retries a build that a transfer error stopped', () => {
  assert.deepStrictEqual(
    run([
      { log: TRANSFER_ERROR, status: 1 },
      { log: 'BUILD SUCCESS', status: 0 }
    ]),
    { status: 0, runs: 2 }
  );
});

test('retries a transfer error next to a test that passed on a rerun', () => {
  assert.deepStrictEqual(
    run([
      { log: `${FLAKY_CLASS}\n${TRANSFER_ERROR}`, status: 1 },
      { log: 'BUILD SUCCESS', status: 0 }
    ]),
    { status: 0, runs: 2 }
  );
});

test('stops after the last attempt when the transfer error persists', () => {
  assert.deepStrictEqual(run([{ log: TRANSFER_ERROR, status: 1 }]), { status: 1, runs: 3 });
});

test('does not retry a build with failing tests next to a transfer error', () => {
  assert.deepStrictEqual(run([{ log: `${TEST_FAILURE}\n${TRANSFER_ERROR}`, status: 1 }]), { status: 1, runs: 1 });
});

test('does not retry a failure that only logged a transfer warning', () => {
  assert.deepStrictEqual(run([{ log: `${TRANSFER_WARNING}\n${OTHER_FAILURE}`, status: 1 }]), {
    status: 1,
    runs: 1
  });
});

test('judges each attempt by its own output', () => {
  assert.deepStrictEqual(
    run([
      { log: TRANSFER_ERROR, status: 1 },
      { log: OTHER_FAILURE, status: 2 }
    ]),
    { status: 2, runs: 2 }
  );
});

test('does not retry when the retry cannot end before the deadline', () => {
  const T = 1000000;
  const attempts = [
    { log: TRANSFER_ERROR, status: 1 },
    { log: TRANSFER_ERROR, status: 1 },
    { log: 'BUILD SUCCESS', status: 0 }
  ];
  const withDeadline = (deadline) => run(attempts, { MAVEN_RETRY_DEADLINE: `${deadline}` }, T);
  // The first attempt fails at T + 100, so its retry would wait 30s and end
  // at T + 230. The second fails at T + 300, so its retry would wait 60s
  // and end at T + 460.
  assert.deepStrictEqual(withDeadline(T + 229), { status: 1, runs: 1 });
  assert.deepStrictEqual(withDeadline(T + 459), { status: 1, runs: 2 });
  assert.deepStrictEqual(withDeadline(T + 460), { status: 0, runs: 3 });
});
