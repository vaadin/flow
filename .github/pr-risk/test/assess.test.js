import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { assess, buildUserMessage, systemPrompt, validateAssessment } from '../assess.js';

const rating = (level) => ({ level, reason: `rated ${level}` });
const VALID = {
  summary: 'Fixes a typo.',
  impact: rating('low'),
  blast_radius: rating('low'),
  uncertainty: rating('low'),
  reversibility: rating('low'),
  level: 'low',
  rationale: 'A documentation fix.',
};

describe('buildUserMessage', () => {
  test('lists the pull request, its linked issues, files and diff, and what the diff misses', () => {
    const message = buildUserMessage({
      pr: { title: 'fix: x', body: 'Fixes #1' },
      files: [{ filename: 'b.js', previous_filename: 'a.js', status: 'renamed', additions: 1, deletions: 2 }],
      diff: { text: '+x', omitted: ['big.bin'] },
      linkedIssues: [{ ref: 'vaadin/flow#1', title: 'Bug', state: 'open', labels: ['Impact: High'] }],
    });
    assert.match(message, /<title>fix: x<\/title>/u);
    assert.match(message, /vaadin\/flow#1 \(open\): Bug \[labels: Impact: High\]/u);
    assert.match(message, /R a\.js -> b\.js \(\+1 -2\)/u);
    assert.match(message, /<missing_diff>\nbig\.bin\n<\/missing_diff>/u);
  });
});

describe('validateAssessment', () => {
  test('accepts an assessment that matches the schema', () => {
    assert.equal(validateAssessment(VALID), VALID);
  });

  test('rejects a level or a text outside the schema', () => {
    for (const invalid of [
      { ...VALID, level: 'green' },
      { ...VALID, impact: rating('severe') },
      { ...VALID, uncertainty: { level: 'low' } },
      { ...VALID, rationale: undefined },
    ]) {
      assert.throws(() => validateAssessment(invalid), /does not match the schema/u);
    }
  });
});

describe('systemPrompt', () => {
  test('gives the context of Flow, the levels and the dimensions', () => {
    const prompt = systemPrompt();
    assert.match(prompt, /Vaadin Flow/u);
    assert.match(prompt, /`Review risk:` line/u);
    assert.match(prompt, /\*\*low\*\*: Can merge without human approval/u);
    assert.match(prompt, /- \*\*blast_radius\*\*: If the implementation is wrong/u);
  });
});

describe('assess', () => {
  test('fails without an API key', async () => {
    await assert.rejects(assess({ pr: {}, files: [], diff: { text: '', omitted: [] }, env: {} }), /ANTHROPIC_API_KEY/u);
  });
});
