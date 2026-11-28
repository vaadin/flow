/**
 * The agent's risk assessment of a pull request: one Claude call that reads
 * the pull request as untrusted text, without tools, and returns the four
 * dimension ratings, each with a reason, and a suggested level.
 */
import { DIMENSIONS, LEVELS } from './classify.js';
import { CONTEXT, DIMENSION_CRITERIA, LEVEL_CRITERIA } from './criteria.js';

// Deliberately hard-coded; each is a one-line edit
const MODEL = 'claude-opus-5-5';
const EFFORT = 'medium';
const MAX_TOKENS = 16000;

// Two attempts of at most five minutes each fit in the job timeout
const CLIENT_OPTIONS = { timeout: 5 * 60 * 1000, maxRetries: 1 };

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
    level: { type: 'string', enum: LEVELS },
    rationale: { type: 'string' },
  },
  required: ['summary', 'impact', 'blast_radius', 'uncertainty', 'reversibility', 'level', 'rationale'],
  additionalProperties: false,
};

const STATUS_LETTERS = { added: 'A', removed: 'D', modified: 'M', renamed: 'R', copied: 'C', changed: 'T' };

function fileLine(file) {
  const status = STATUS_LETTERS[file.status] ?? '?';
  const name = file.previous_filename ? `${file.previous_filename} -> ${file.filename}` : file.filename;
  return `${status} ${name} (+${file.additions} -${file.deletions})`;
}

function issueLine({ ref, title, state, labels }) {
  return `${ref} (${state}): ${title} [labels: ${labels.length > 0 ? labels.join(', ') : 'none'}]`;
}

export function buildUserMessage({ pr, files, diff, linkedIssues = [] }) {
  const description = (pr.body ?? '').slice(0, DESCRIPTION_BUDGET);
  const sections = [
    `<title>${pr.title}</title>`,
    `<description>\n${description}\n</description>`,
    ...(linkedIssues.length > 0
      ? [`<linked_issues>\n${linkedIssues.map(issueLine).join('\n')}\n</linked_issues>`]
      : []),
    `<changed_files>\n${files.map(fileLine).join('\n')}\n</changed_files>`,
    `<diff>\n${diff.text}\n</diff>`,
  ];
  if (diff.omitted.length > 0) {
    sections.push(`<missing_diff>\n${diff.omitted.join('\n')}\n</missing_diff>`);
  }
  return sections.join('\n\n');
}

/**
 * The assessment, when it matches the schema. The API does not enforce every
 * part of it: the Anthropic SDK turns enums into descriptions, for example.
 */
export function validateAssessment(value) {
  const isText = (field) => typeof field === 'string';
  const isRating = (rating) => LEVELS.includes(rating?.level) && isText(rating.reason);
  const valid =
    LEVELS.includes(value?.level) &&
    isText(value.summary) &&
    isText(value.rationale) &&
    DIMENSIONS.every(({ key }) => isRating(value[key]));
  if (!valid) {
    throw new Error('The assessment does not match the schema');
  }
  return value;
}

export function systemPrompt() {
  return [
    'You classify pull requests by risk, so that the repository knows how much human review a pull request needs before it is merged.',
    CONTEXT,
    '## Levels',
    ...LEVEL_CRITERIA.choices.map(({ value, description }) => `**${value}**: ${description}`),
    LEVEL_CRITERIA.question,
    '## Dimensions',
    'Rate each dimension low, medium or high, where high is the riskier end, with one sentence of reason that names the code behind the rating:',
    DIMENSIONS.map(({ key }) => {
      const { question, low, medium, high } = DIMENSION_CRITERIA[key];
      return `- **${key}**: ${question} Low: ${low}${medium ? ` Medium: ${medium}` : ''} High: ${high}`;
    }).join('\n'),
    '## Output',
    'Respond with a summary of one sentence on what the pull request changes, the four ratings, the level, and a rationale of at most three sentences that a reviewer can check against the diff.',
  ].join('\n\n');
}

/**
 * The assessment with the `model` that made it. Throws when ANTHROPIC_API_KEY
 * is not set, or the call fails or ends without a valid assessment, so that a
 * lost assessment is never mistaken for one. The SDK is installed only by the
 * workflow (npm ci in this directory), so that the tests run without it.
 */
export async function assess({ pr, files, diff, linkedIssues = [], env = process.env }) {
  if (!env.ANTHROPIC_API_KEY) {
    throw new Error('ANTHROPIC_API_KEY is not set');
  }
  const { default: Anthropic } = await import('@anthropic-ai/sdk');
  const { jsonSchemaOutputFormat } = await import('@anthropic-ai/sdk/helpers/json-schema');

  // The schema in the form that the API supports
  const { type, schema } = jsonSchemaOutputFormat(ASSESSMENT_SCHEMA);
  const client = new Anthropic({ apiKey: env.ANTHROPIC_API_KEY, ...CLIENT_OPTIONS });
  const response = await client.beta.messages.create({
    model: MODEL,
    max_tokens: MAX_TOKENS,
    // A request that the safety classifiers decline runs again on the
    // fallback model that Anthropic recommends for the refusal category
    betas: ['server-side-fallback-2026-07-01'],
    fallbacks: 'default',
    output_config: { effort: EFFORT, format: { type, schema } },
    system: systemPrompt(),
    messages: [{ role: 'user', content: buildUserMessage({ pr, files, diff, linkedIssues }) }],
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
