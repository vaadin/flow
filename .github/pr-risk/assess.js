/**
 * The agent's risk assessment of a pull request: one model call that reads the
 * pull request as untrusted text, without tools, and returns the four
 * dimension ratings and a suggested category. Uses the first provider whose
 * key is set:
 * - OPENAI_API_KEY: the OpenAI Decisions API, which answers in seconds with a
 *   probability for each rating,
 * - ANTHROPIC_API_KEY: Claude, which also explains each rating,
 * - OPENROUTER_API_KEY: a model on OpenRouter, OPENROUTER_MODEL or the
 *   default below, which also explains each rating.
 */
import { CATEGORIES, DIMENSIONS } from './classify.js';
import { CATEGORY_CRITERIA, DIMENSION_CRITERIA, repositoryContext } from './criteria.js';

// Deliberately hard-coded; each is a one-line edit
// The only model that the Decisions API supports
const DECISIONS_MODEL = 'gpt-6-luna';
const CLAUDE_MODEL = 'claude-opus-5-5';
// OPENROUTER_MODEL overrides it
const OPENROUTER_MODEL = 'openai/gpt-6.1-sol';
const OPENROUTER_URL = 'https://openrouter.ai/api/v1';
const EFFORT = 'medium';
const MAX_TOKENS = 16000;

// Two attempts of at most five minutes each fit in the job timeout
const CLIENT_OPTIONS = { timeout: 5 * 60 * 1000, maxRetries: 1 };

const LEVELS = ['low', 'medium', 'high'];

/** Characters of the description that the agent reads */
const DESCRIPTION_BUDGET = 20_000;

const RATING = {
  type: 'object',
  properties: {
    level: { type: 'string', enum: LEVELS },
    reason: { type: 'string' },
  },
  required: ['level', 'reason'],
  additionalProperties: false,
};

export const ASSESSMENT_SCHEMA = {
  type: 'object',
  properties: {
    summary: { type: 'string' },
    impact: RATING,
    blast_radius: RATING,
    uncertainty: RATING,
    reversibility: RATING,
    category: { type: 'string', enum: CATEGORIES },
    rationale: { type: 'string' },
  },
  required: ['summary', 'impact', 'blast_radius', 'uncertainty', 'reversibility', 'category', 'rationale'],
  additionalProperties: false,
};

const STATUS_LETTERS = { added: 'A', removed: 'D', modified: 'M', renamed: 'R', copied: 'C', changed: 'T' };

function fileLine(file) {
  const status = STATUS_LETTERS[file.status] ?? '?';
  const name = file.previous_filename ? `${file.previous_filename} -> ${file.filename}` : file.filename;
  return `${status} ${name} (+${file.additions} -${file.deletions})`;
}

export function buildUserMessage({ pr, files, diff }) {
  const description = (pr.body ?? '').slice(0, DESCRIPTION_BUDGET);
  const sections = [
    `<title>${pr.title}</title>`,
    `<description>\n${description}\n</description>`,
    `<changed_files>\n${files.map(fileLine).join('\n')}\n</changed_files>`,
    `<diff>\n${diff.text}\n</diff>`,
  ];
  if (diff.omitted.length > 0) {
    sections.push(`<missing_diff>\n${diff.omitted.join('\n')}\n</missing_diff>`);
  }
  return sections.join('\n\n');
}

/**
 * The assessment, when it matches the schema. The APIs do not enforce every
 * part of it: the Anthropic SDK turns enums into descriptions, for example. An
 * off-schema category would otherwise reach the labels. The texts come only
 * from Claude, the probabilities only from the Decisions API.
 */
export function validateAssessment(value) {
  const isOptionalText = (field) => field === undefined || typeof field === 'string';
  const isOptionalProbabilities = (field) =>
    field === undefined ||
    (typeof field === 'object' &&
      field !== null &&
      Object.values(field).every(
        (probability) => typeof probability === 'number' && probability >= 0 && probability <= 1,
      ));
  const isRating = (rating) =>
    LEVELS.includes(rating?.level) && isOptionalText(rating.reason) && isOptionalProbabilities(rating.probabilities);
  const valid =
    CATEGORIES.includes(value?.category) &&
    isOptionalProbabilities(value.probabilities) &&
    isOptionalText(value.summary) &&
    isOptionalText(value.rationale) &&
    DIMENSIONS.every(({ key }) => isRating(value[key]));
  if (!valid) {
    throw new Error('The assessment does not match the schema');
  }
  return value;
}

/**
 * The questions for the Decisions API: a score per dimension, with the levels
 * in the order of LEVELS, and a choice of the category. The `context` of the
 * repository and the criteria go into every question, apart from the
 * untrusted pull request in the input.
 */
export function decisionQuestions(context) {
  const dimensions = DIMENSIONS.map(({ key, title }) => {
    const { question, ...levels } = DIMENSION_CRITERIA[key];
    return {
      name: key,
      type: 'score',
      instructions: `${context}\n\nRate the ${title.toLowerCase()} of the pull request: ${question}`,
      levels: LEVELS.map((label) => ({ label, description: levels[label] ?? 'Between low and high.' })),
    };
  });
  const category = {
    name: 'category',
    type: 'choice',
    instructions: `${context}\n\n${CATEGORY_CRITERIA.question}`,
    choices: CATEGORY_CRITERIA.choices,
  };
  return [...dimensions, category];
}

/** The probability of each level label or choice value of an answer */
function probabilitiesOf(answer) {
  return Object.fromEntries(
    (answer?.probabilities ?? []).map(({ label, value, probability }) => [label ?? value, probability]),
  );
}

/**
 * The assessment in a Decisions API response. Each dimension gets its most
 * likely level. The `score` of an answer is not used: it is the average of
 * the level indexes, weighted by their probabilities.
 */
export function decisionToAssessment(response) {
  const refused = response.answers.filter((answer) => answer.type === 'refusal');
  if (refused.length > 0) {
    throw new Error(`${response.model} declined to answer ${refused.map((answer) => answer.name).join(', ')}`);
  }
  const answers = Object.fromEntries(response.answers.map((answer) => [answer.name, answer]));
  const ratings = DIMENSIONS.map(({ key }) => {
    const probabilities = probabilitiesOf(answers[key]);
    // A tie goes to the riskier level
    const level = LEVELS.reduce(
      (best, label) => (probabilities[label] >= (probabilities[best] ?? -1) ? label : best),
      null,
    );
    return [key, { level, probabilities }];
  });
  const assessment = {
    ...Object.fromEntries(ratings),
    category: answers.category?.choice,
    probabilities: probabilitiesOf(answers.category),
  };
  return { ...validateAssessment(assessment), model: response.model };
}

/** The system prompt for the providers that explain their ratings, with the `context` of the repository */
export function systemPrompt(context) {
  return [
    'You classify pull requests by risk, so that the repository knows how much human review a pull request needs before it is merged.',
    context,
    '## Categories',
    ...CATEGORY_CRITERIA.choices.map(({ value, description }) => `**${value}**: ${description}`),
    CATEGORY_CRITERIA.question,
    '## Dimensions',
    'Rate each dimension low, medium or high, where high is the riskier end, with one sentence of reason that names the code behind the rating:',
    DIMENSIONS.map(({ key }) => {
      const { question, low, high } = DIMENSION_CRITERIA[key];
      return `- **${key}**: ${question} Low: ${low} High: ${high}`;
    }).join('\n'),
    '## Output',
    'Respond with a summary of one sentence on what the pull request changes, the four ratings, the category, and a rationale of at most three sentences that a reviewer can check against the diff.',
  ].join('\n\n');
}

/** The npm package of the provider that `assess` uses with these environment variables, or null without a key */
export function providerPackage(env) {
  if (env.OPENAI_API_KEY) {
    return 'openai';
  }
  if (env.ANTHROPIC_API_KEY) {
    return '@anthropic-ai/sdk';
  }
  // OpenRouter serves its models through the OpenAI chat completions API
  return env.OPENROUTER_API_KEY ? 'openai' : null;
}

/** The assessment in a chat completion response of OpenRouter */
export function chatToAssessment(response) {
  const choice = response.choices?.[0];
  if (!choice) {
    throw new Error(`OpenRouter returned no answer${response.error ? `: ${response.error.message}` : ''}`);
  }
  if (choice.message?.refusal) {
    throw new Error(`${response.model} refused: ${choice.message.refusal}`);
  }
  // Checked before parsing, so that a cut-off answer says so
  if (choice.finish_reason !== 'stop') {
    throw new Error(`${response.model} stopped with ${choice.finish_reason}`);
  }
  return { ...validateAssessment(JSON.parse(choice.message.content)), model: response.model };
}

// The SDKs are installed only by the workflow (npm ci in this directory), so
// that the policy and its tests run without them

/** The untrusted input of a pull request and the context of its repository */
function promptParts({ pr, files, diff }) {
  return { content: buildUserMessage({ pr, files, diff }), context: repositoryContext(pr.base.repo) };
}

async function assessWithDecisions(pullRequest, apiKey) {
  const { content, context } = promptParts(pullRequest);
  const { default: OpenAI } = await import('openai');

  const client = new OpenAI({ apiKey, ...CLIENT_OPTIONS });
  const response = await client.decisions.create({
    model: DECISIONS_MODEL,
    input: [{ role: 'user', content }],
    questions: decisionQuestions(context),
  });
  return decisionToAssessment(response);
}

async function assessWithClaude(pullRequest, apiKey) {
  const { content, context } = promptParts(pullRequest);
  const { default: Anthropic } = await import('@anthropic-ai/sdk');
  const { jsonSchemaOutputFormat } = await import('@anthropic-ai/sdk/helpers/json-schema');

  // The schema in the form that the API supports
  const { type, schema } = jsonSchemaOutputFormat(ASSESSMENT_SCHEMA);
  const client = new Anthropic({ apiKey, ...CLIENT_OPTIONS });
  const response = await client.beta.messages.create({
    model: CLAUDE_MODEL,
    max_tokens: MAX_TOKENS,
    // A request that the safety classifiers decline runs again on the
    // fallback model that Anthropic recommends for the refusal category
    betas: ['server-side-fallback-2026-07-01'],
    fallbacks: 'default',
    output_config: { effort: EFFORT, format: { type, schema } },
    system: systemPrompt(context),
    messages: [{ role: 'user', content }],
  });

  // Checked before parsing, so that a refusal or a cut-off answer says so
  if (response.stop_reason !== 'end_turn') {
    const category = response.stop_details?.category;
    throw new Error(`${response.model} stopped with ${response.stop_reason}${category ? ` (${category})` : ''}`);
  }
  const output = response.content.find((block) => block.type === 'text');
  if (!output) {
    throw new Error(`${response.model} returned no assessment`);
  }
  return { ...validateAssessment(JSON.parse(output.text)), model: response.model };
}

async function assessWithOpenRouter(pullRequest, apiKey, model) {
  const { content, context } = promptParts(pullRequest);
  const { default: OpenAI } = await import('openai');

  const client = new OpenAI({ apiKey, baseURL: OPENROUTER_URL, ...CLIENT_OPTIONS });
  const response = await client.chat.completions.create({
    model,
    max_tokens: MAX_TOKENS,
    reasoning: { effort: EFFORT },
    response_format: {
      type: 'json_schema',
      json_schema: { name: 'assessment', strict: true, schema: ASSESSMENT_SCHEMA },
    },
    // Only route to providers that support the JSON schema
    provider: { require_parameters: true },
    messages: [
      { role: 'system', content: systemPrompt(context) },
      { role: 'user', content },
    ],
  });
  return chatToAssessment(response);
}

/**
 * The assessment with the `model` that made it. Throws when no API key is set,
 * or the call fails or ends without a valid assessment, so that a lost
 * assessment is never mistaken for one.
 */
export function assess({ pr, files, diff, env = process.env }) {
  if (env.OPENAI_API_KEY) {
    return assessWithDecisions({ pr, files, diff }, env.OPENAI_API_KEY);
  }
  if (env.ANTHROPIC_API_KEY) {
    return assessWithClaude({ pr, files, diff }, env.ANTHROPIC_API_KEY);
  }
  if (env.OPENROUTER_API_KEY) {
    const model = env.OPENROUTER_MODEL || OPENROUTER_MODEL;
    return assessWithOpenRouter({ pr, files, diff }, env.OPENROUTER_API_KEY, model);
  }
  return Promise.reject(new Error('None of OPENAI_API_KEY, ANTHROPIC_API_KEY and OPENROUTER_API_KEY is set'));
}
