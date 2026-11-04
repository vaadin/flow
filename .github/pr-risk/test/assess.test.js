import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import {
  assess,
  chatToAssessment,
  decisionQuestions,
  decisionToAssessment,
  providerPackage,
  validateAssessment,
} from '../assess.js';
import { repositoryContext } from '../criteria.js';

const VALID = {
  summary: 'Fixes a typo.',
  impact: { level: 'low', reason: 'r' },
  blast_radius: { level: 'low', reason: 'r' },
  uncertainty: { level: 'medium', reason: 'r' },
  reversibility: { level: 'high', reason: 'r' },
  category: 'amber',
  rationale: 'x',
};

/** A score answer as the API returns it: `score` is the weighted average of the level indexes */
function score(name, low, medium, high) {
  const probabilities = [
    { value: 0, label: 'low', probability: low },
    { value: 1, label: 'medium', probability: medium },
    { value: 2, label: 'high', probability: high },
  ];
  return { type: 'score', name, score: medium + 2 * high, confidence: 0.5, probabilities };
}

function decision(answers) {
  return { model: 'gpt-6-luna', answers, usage: {} };
}

const CATEGORY = {
  type: 'choice',
  name: 'category',
  choice: 'amber',
  confidence: 0.7,
  probabilities: [
    { value: 'green', probability: 0.1 },
    { value: 'amber', probability: 0.8 },
    { value: 'red', probability: 0.1 },
  ],
};

const ANSWERS = [
  score('impact', 0.92, 0.05, 0.03),
  // Most likely low, although the weighted score of 0.54 is closer to medium
  score('blast_radius', 0.53, 0.4, 0.07),
  // A tie goes to the riskier level
  score('uncertainty', 0.4, 0.4, 0.2),
  score('reversibility', 0.1, 0.3, 0.6),
  CATEGORY,
];

describe('validateAssessment', () => {
  test('returns a valid assessment, with reasons or with confidences', () => {
    assert.equal(validateAssessment(VALID), VALID);
    const decided = {
      impact: { level: 'low', probabilities: { low: 1, medium: 0, high: 0 } },
      blast_radius: { level: 'low', probabilities: { low: 0.5, medium: 0.5, high: 0 } },
      uncertainty: { level: 'medium', probabilities: { low: 0, medium: 0.9, high: 0.1 } },
      reversibility: { level: 'high', probabilities: { low: 0, medium: 0, high: 1 } },
      category: 'amber',
      probabilities: { green: 0.2, amber: 0.8, red: 0 },
    };
    assert.equal(validateAssessment(decided), decided);
  });

  test('rejects a category, level, text or confidence outside the schema', () => {
    const invalid = [
      { ...VALID, category: 'none' },
      { ...VALID, category: 'Green' },
      { ...VALID, impact: { level: 'none', reason: 'r' } },
      { ...VALID, blast_radius: undefined },
      { ...VALID, rationale: 42 },
      { ...VALID, probabilities: { green: 1.5 } },
      { ...VALID, probabilities: 0.9 },
      { ...VALID, impact: { level: 'low', probabilities: { low: '90%' } } },
      null,
    ];
    for (const value of invalid) {
      assert.throws(() => validateAssessment(value), { message: 'The assessment does not match the schema' });
    }
  });
});

describe('decisionQuestions', () => {
  test('asks for a score per dimension, from low to high, and for the category', () => {
    const questions = decisionQuestions('The context.');
    assert.deepEqual(
      questions.map(({ name, type }) => `${type} ${name}`),
      ['score impact', 'score blast_radius', 'score uncertainty', 'score reversibility', 'choice category'],
    );
    assert.deepEqual(
      questions[0].levels.map(({ label }) => label),
      ['low', 'medium', 'high'],
    );
    assert.deepEqual(
      questions[4].choices.map(({ value }) => value),
      ['green', 'amber', 'red'],
    );
    assert.ok(questions.every(({ instructions }) => instructions.startsWith('The context.\n\n')));
  });
});

describe('decisionToAssessment', () => {
  test('rates each dimension by its most likely level', () => {
    assert.deepEqual(decisionToAssessment(decision(ANSWERS)), {
      impact: { level: 'low', probabilities: { low: 0.92, medium: 0.05, high: 0.03 } },
      blast_radius: { level: 'low', probabilities: { low: 0.53, medium: 0.4, high: 0.07 } },
      uncertainty: { level: 'medium', probabilities: { low: 0.4, medium: 0.4, high: 0.2 } },
      reversibility: { level: 'high', probabilities: { low: 0.1, medium: 0.3, high: 0.6 } },
      category: 'amber',
      probabilities: { green: 0.1, amber: 0.8, red: 0.1 },
      model: 'gpt-6-luna',
    });
  });

  test('fails when a question is refused or missing', () => {
    const refused = [...ANSWERS.slice(0, 4), { type: 'refusal', name: 'category' }];
    assert.throws(() => decisionToAssessment(decision(refused)), { message: 'gpt-6-luna declined to answer category' });
    assert.throws(() => decisionToAssessment(decision(ANSWERS.slice(1))), {
      message: 'The assessment does not match the schema',
    });
  });
});

describe('providerPackage', () => {
  test('prefers OpenAI, then Claude, then OpenRouter', () => {
    assert.equal(providerPackage({ ANTHROPIC_API_KEY: 'a', OPENAI_API_KEY: 'o', OPENROUTER_API_KEY: 'r' }), 'openai');
    assert.equal(providerPackage({ ANTHROPIC_API_KEY: 'a', OPENROUTER_API_KEY: 'r' }), '@anthropic-ai/sdk');
    assert.equal(providerPackage({ OPENROUTER_API_KEY: 'r' }), 'openai');
    assert.equal(providerPackage({}), null);
  });
});

describe('assess', () => {
  test('fails without an API key', async () => {
    const pr = { title: 't', body: '' };
    await assert.rejects(assess({ pr, files: [], diff: { text: '', omitted: [] }, env: {} }), {
      message: 'None of OPENAI_API_KEY, ANTHROPIC_API_KEY and OPENROUTER_API_KEY is set',
    });
  });
});

describe('repositoryContext', () => {
  test('describes this repository in its own words', () => {
    const context = repositoryContext({ full_name: 'vaadin/web-components', description: 'Web components' });
    assert.match(context, /^The pull request is to the Vaadin web components repository, a monorepo/u);
    assert.doesNotMatch(context, /Web components/u);
  });

  test('describes other repositories by their GitHub name, description and language', () => {
    const context = repositoryContext({
      full_name: 'vaadin/flow-components',
      description: 'Java counterpart of Vaadin Web Components',
      language: 'Java',
    });
    assert.match(
      context,
      /^The pull request is to the vaadin\/flow-components repository on GitHub\. Java counterpart of Vaadin Web Components Its main language is Java\.\n\n/u,
    );
    assert.match(
      repositoryContext({ full_name: 'a/b', description: null, language: null }),
      /on GitHub\.\n\nThe pull/u,
    );
  });
});

describe('chatToAssessment', () => {
  function chat(choice) {
    return { model: 'openai/gpt-6.1-sol', choices: [{ finish_reason: 'stop', ...choice }] };
  }

  test('parses the JSON answer and adds the model', () => {
    const response = chat({ message: { role: 'assistant', content: JSON.stringify(VALID) } });
    assert.deepEqual(chatToAssessment(response), { ...VALID, model: 'openai/gpt-6.1-sol' });
  });

  test('fails on a refusal, a cut-off answer, an answer outside the schema or an error', () => {
    const cases = [
      [chat({ message: { content: null, refusal: 'No.' } }), 'openai/gpt-6.1-sol refused: No.'],
      [chat({ finish_reason: 'length', message: { content: '{"summ' } }), 'openai/gpt-6.1-sol stopped with length'],
      [
        chat({ message: { content: JSON.stringify({ ...VALID, category: 'none' }) } }),
        'The assessment does not match the schema',
      ],
      [{ error: { message: 'No endpoints found' } }, 'OpenRouter returned no answer: No endpoints found'],
    ];
    for (const [response, message] of cases) {
      assert.throws(() => chatToAssessment(response), { message });
    }
  });
});
