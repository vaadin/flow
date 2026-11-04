/**
 * The repository policy of the PR risk workflow (.github/workflows/pr-risk.yml).
 * Every rule that matches a pull request sets a minimum category for it. The
 * agent can suggest a higher category than the policy, never a lower one.
 *
 * The generic part applies to every repository. The part of a repository adds
 * rules that only make sense there, and says which of its files are shipped
 * code and which are screenshots. The workflow runs in vaadin/web-components
 * and vaadin/flow; the command line also classifies pull requests of other
 * repositories, with the generic part alone.
 *
 * The workflow runs the policy of the base branch, so a pull request that edits
 * this file is classified by the policy it replaces.
 */

/**
 * Path rules match when one of their globs matches the path of a changed file,
 * or the previous path of a renamed file, unless `exceptPaths` matches all of
 * its paths. Wildcards also match names that
 * start with a dot. A rule with `codeOnly` skips files whose changes are only
 * comments and blank lines, so that a JSDoc fix does not need a reviewer. A
 * rule with `removed` sets its lower category for files that the pull request
 * removes and that match its paths. A rule with `checksOnly` sets its lower
 * category for files that match its paths, but not its `exceptPaths`, when no
 * added or removed line matches `sensitive`.
 *
 * Content rules match lines that a pull request adds to shipped code: the
 * files that match `contentPaths` but not `testPaths`.
 */
const GENERIC = {
  pathRules: [
    {
      category: 'red',
      reason: 'CI, release and repository automation, or config that runs code',
      // A removed workflow runs nothing, but CI may rely on it, so it still needs one approval
      removed: {
        category: 'amber',
        reason: 'removes CI workflows',
        paths: ['.github/workflows/**'],
      },
      paths: ['.github/**', '.husky/**', '**/.yarnrc*', '**/.npmrc', '**/.gitattributes'],
    },
    {
      category: 'amber',
      reason: 'dependencies',
      // A patch update of a build or test tool, see `patchBumps`
      exceptPatchBumps: true,
      paths: [
        '**/package.json',
        '**/yarn.lock',
        '**/package-lock.json',
        '**/pnpm-lock.yaml',
        '**/pom.xml',
        '**/build.gradle*',
        '**/settings.gradle*',
      ],
    },
    {
      category: 'amber',
      reason: 'instructions that agents follow, including the agent review',
      paths: ['**/CLAUDE.md', '**/CLAUDE.local.md', '**/AGENTS.md', '**/.claude/**', '.mcp.json'],
    },
    {
      category: 'amber',
      reason: 'test, lint and build tooling that CI relies on',
      paths: ['*.config.{js,mjs,cjs,ts}', '**/tsconfig*.json', '.prettier*', '.stylelintrc*', '.eslintrc*'],
    },
  ],
  // Without more knowledge of a repository, all of it but its tests is shipped code
  contentPaths: ['**'],
  testPaths: ['**/test/**', '**/tests/**', '**/__tests__/**', '**/*.{test,spec}.*'],
  screenshotPaths: [],
  issueLabelRules: [],
  patchBumps: null,
};

/**
 * The parts of the repositories. Path rules with the category and reason of a
 * generic rule add their paths to it.
 */
const REPOSITORIES = {
  'vaadin/web-components': {
    pathRules: [
      {
        category: 'red',
        reason: 'CI, release and repository automation, or config that runs code',
        paths: ['scripts/**', 'lerna.json', 'patches/**'],
      },
      {
        category: 'red',
        reason: 'HTML sanitization',
        paths: ['packages/markdown/src/**', 'packages/rich-text-editor/src/**', 'packages/rich-text-editor/vendor/**'],
      },
      {
        category: 'red',
        reason: 'authentication UI',
        paths: ['packages/login/src/**'],
      },
      {
        category: 'red',
        reason: 'file uploads to the server',
        paths: ['packages/upload/src/vaadin-upload-mixin.js', 'packages/upload/src/vaadin-upload-*manager.js'],
      },
      {
        category: 'red',
        reason: 'usage statistics',
        paths: ['packages/component-base/src/element-mixin.js'],
      },
      {
        category: 'amber',
        reason: 'instructions that agents follow, including the agent review',
        paths: ['CONVENTIONS.md', 'guidelines/**'],
      },
      {
        category: 'amber',
        reason: 'test, lint and build tooling that CI relies on',
        paths: ['wtr-utils.js', 'wds-utils.js', 'custom-rules/**', 'manifest/**', 'react/scripts/**'],
      },
      {
        category: 'amber',
        reason: 'shared base packages that most components build on',
        paths: [
          'packages/component-base/src/**',
          'packages/a11y-base/src/**',
          'packages/field-base/src/**',
          'packages/overlay/src/**',
          'packages/vaadin-themable-mixin/*.js',
          'packages/vaadin-themable-mixin/src/**',
        ],
      },
      {
        category: 'amber',
        reason: 'themes that every application loads',
        paths: [
          'packages/aura/*.css',
          'packages/aura/src/**',
          'packages/vaadin-lumo-styles/*.{css,js}',
          'packages/vaadin-lumo-styles/!(test)/**',
        ],
      },
      {
        category: 'amber',
        reason: 'visual test screenshots, so the rendering changed',
        paths: ['packages/*/test/visual/**/screenshots/**'],
      },
      {
        category: 'amber',
        reason: 'public TypeScript API',
        paths: ['packages/*/*.d.ts', 'packages/*/src/**/*.d.ts'],
        codeOnly: true,
      },
    ],
    contentPaths: ['packages/*/src/**', 'packages/*/*.js'],
    testPaths: [],
    screenshotPaths: ['packages/*/test/visual/**/screenshots/**'],
  },
  'vaadin/flow': {
    pathRules: [
      {
        category: 'red',
        reason: 'CI, release and repository automation, or config that runs code',
        paths: ['scripts/**', 'build-tools/**', 'release-check/**', 'flow-bom/**', '.mvn/**'],
        // Workflows and scripts that only check pull requests, such as the API
        // diff or a retry, cannot reach a secret or a release unless they change
        // the lines that grant that power. The merge gates (the PR risk
        // workflow, the API diff that sets the `+0.0.1` label and the
        // auto-merge), the agent workflows and the scripts that push with a
        // token stay red.
        checksOnly: {
          category: 'amber',
          reason: 'CI checks that change no secrets, permissions, triggers or actions',
          paths: ['.github/workflows/**', 'scripts/**'],
          exceptPaths: [
            '.github/workflows/pr-risk*.yml',
            '.github/workflows/*.md',
            '.github/workflows/*.lock.yml',
            'scripts/autoMerge.js',
            'scripts/mergePr.sh',
            'scripts/cherryPick.js',
            'scripts/apiDiff*',
            'scripts/ChangeTypeLevels*',
          ],
          sensitive: new RegExp(
            [
              String.raw`secrets\.|\b\w*(?:TOKEN|PASSWORD|API_KEY)\b`,
              String.raw`^\s*(?:permissions|on|pull_request_target|workflow_run|issue_comment)\s*:`,
              // A permission inside a `permissions:` block, such as `contents: write`
              String.raw`^\s*[\w-]+\s*:\s*(?:write|read|none|write-all|read-all)\s*$`,
              String.raw`^\s*(?:-\s*)?uses\s*:`,
              // The API diff and auto-merge steps: the `+0.0.1` label lets a pull
              // request be approved and merged without a human
              String.raw`\+0\.0\.1|\+0\.1\.0|\+1\.0\.0|version_change|api-?[dD]iff|apicmp|japicmp|auto-?[mM]erge`,
              // Text from the pull request inserted into a script
              String.raw`\$\{\{\s*(?:github\.event|github\.head_ref|inputs)`,
            ].join('|'),
            'mu',
          ),
        },
        // Documentation that no workflow reads
        exceptPaths: [
          '.github/workflows/README.md',
          '.github/RELEASE_DIGEST.md',
          '.github/ISSUE_TEMPLATE.md',
          '.github/ISSUE_TEMPLATE/**',
        ],
      },
      {
        category: 'red',
        reason: 'access control, authentication and CSRF protection',
        // The security rules skip Javadoc changes, such as a new `@since` tag
        codeOnly: true,
        paths: [
          'flow-server/src/main/java/com/vaadin/flow/server/auth/**',
          'flow-server/src/main/java/com/vaadin/flow/internal/springcsrf/**',
          'vaadin-spring/src/main/java/com/vaadin/flow/spring/security/**',
        ],
      },
      {
        category: 'red',
        reason: 'handling of client requests and RPC input',
        codeOnly: true,
        paths: [
          'flow-server/src/main/java/com/vaadin/flow/server/HandlerHelper.java',
          'flow-server/src/main/java/com/vaadin/flow/server/StaticFileServer.java',
          'flow-server/src/main/java/com/vaadin/flow/server/communication/ServerRpcHandler.java',
          'flow-server/src/main/java/com/vaadin/flow/server/communication/UidlRequestHandler.java',
          'flow-server/src/main/java/com/vaadin/flow/server/communication/rpc/**',
        ],
      },
      {
        category: 'red',
        reason: 'file uploads to the server',
        codeOnly: true,
        paths: [
          'flow-server/src/main/java/com/vaadin/flow/server/communication/StreamReceiverHandler.java',
          'flow-server/src/main/java/com/vaadin/flow/server/communication/TransferUtil.java',
          'flow-server/src/main/java/com/vaadin/flow/server/streams/*Upload*.java',
        ],
      },
      {
        category: 'red',
        reason: 'usage statistics',
        codeOnly: true,
        paths: [
          'flow-server/src/main/java/com/vaadin/flow/internal/UsageStatistics*.java',
          'flow-server/src/main/java/com/vaadin/flow/server/UsageStatisticsCollector.java',
          'vaadin-dev-server/src/main/java/com/vaadin/base/devserver/stats/**',
        ],
      },
      {
        category: 'red',
        reason: 'licensing',
        codeOnly: true,
        paths: ['**/src/main/**/*LicenseChecker*'],
      },
      {
        category: 'amber',
        reason: 'dependencies',
        // Integration test modules, which no published artifact contains
        paths: [],
        exceptPaths: ['flow-tests/**'],
      },
      {
        category: 'amber',
        reason: 'instructions that agents follow, including the agent review',
        paths: ['CONVENTIONS.md', 'guidelines/**'],
      },
      {
        category: 'amber',
        reason: 'test, lint and build tooling that CI relies on',
        paths: ['checkstyle/**', 'eclipse/**', 'flow-test-generic/**', 'flow-test-util/**', '.prettierrc*'],
      },
      {
        category: 'amber',
        reason: 'core framework code that every application runs',
        paths: [
          'flow-server/src/main/**',
          'flow-client/src/main/**',
          'flow-data/src/main/**',
          'signals/src/main/**',
          'flow-push/src/main/**',
        ],
        codeOnly: true,
      },
      {
        category: 'amber',
        reason: 'Spring, CDI and Quarkus integration that every application on that stack runs',
        paths: ['vaadin-spring/src/main/**', 'vaadin-cdi/src/main/**', 'vaadin-quarkus/*/src/main/**'],
        codeOnly: true,
      },
      {
        category: 'amber',
        reason: 'build plugins and dev tooling that every application build runs',
        paths: [
          'flow-plugins/*/src/main/**',
          'flow-build-tools/src/main/**',
          'vaadin-dev-server/src/main/**',
          'flow-devloop-daemon/src/main/**',
          'flow-server-production-mode/src/main/**',
        ],
        codeOnly: true,
      },
    ],
    contentPaths: ['**/src/main/**'],
    testPaths: [
      'flow-tests/**',
      '**/src/test/**',
      '**/src/it/**',
      'flow-test-generic/**',
      'flow-test-util/**',
      'flow-html-components-testbench/**',
    ],
    screenshotPaths: [],
    issueLabelRules: [
      {
        label: 'Impact: High',
        category: 'amber',
        reason: 'a linked issue is labelled `Impact: High`: the problem reaches many applications',
      },
    ],
    // Dependabot names a dependency `deps-dev` when no shipped artifact uses
    // it, such as a test or build plugin. A patch update of one is safe to
    // merge once CI passes, so the dependency rules (those with
    // `exceptPatchBumps`) skip it. Updates of shipped dependencies, minor and
    // major updates, and pull requests that change more than the version
    // still need a human approval.
    patchBumps: {
      reason: 'a patch update of a build or test tool by Dependabot',
      authors: ['dependabot[bot]'],
      title: /^chore\(deps-dev\): bump \S+ from (\d+)\.(\d+)\.(\d+) to (\d+)\.(\d+)\.(\d+)$/u,
    },
  },
};

/**
 * The policy of a repository, `owner/name`: the generic path rules together
 * with its own, and its own shipped code and screenshot paths when it has a part.
 */
export function policyFor(repository) {
  const own = REPOSITORIES[repository];
  if (!own) {
    return GENERIC;
  }
  const pathRules = GENERIC.pathRules.map((rule) => ({ ...rule, paths: [...rule.paths] }));
  for (const rule of own.pathRules) {
    const generic = pathRules.find(({ category, reason }) => category === rule.category && reason === rule.reason);
    if (generic) {
      generic.paths.push(...rule.paths);
      generic.exceptPaths = [...(generic.exceptPaths ?? []), ...(rule.exceptPaths ?? [])];
      generic.checksOnly = rule.checksOnly ?? generic.checksOnly;
    } else {
      pathRules.push(rule);
    }
  }
  return { ...own, pathRules, issueLabelRules: own.issueLabelRules ?? [], patchBumps: own.patchBumps ?? null };
}

/**
 * Rules on the lines that a pull request adds to shipped code. Comment lines
 * count too, as code can follow a comment on the same line, apart from Java,
 * where a comment-only line holds no code. Lines that match `except` are
 * skipped, and so are files that match `exceptPaths`. With `javaStringsOnly`,
 * Java lines are only checked in their string
 * literals: Java has no such API of its own, so it only matters in JavaScript
 * that Java code sends to the browser, such as `executeJs("fetch(...)")`.
 */
export const CONTENT_RULES = [
  {
    category: 'red',
    reason: 'adds an HTML or script injection sink',
    // Comparing the content, or clearing it with `innerHTML = ''`, is common and safe
    pattern: new RegExp(
      [
        String.raw`\b(?:inner|outer)HTML\b(?!\s*(?:[!=]==?|=\s*(?:''|""|${'``'})\s*;?\s*$))`,
        String.raw`insertAdjacentHTML|setHTMLUnsafe|parseHTMLUnsafe|createContextualFragment|\bsrcdoc\b`,
        String.raw`\bunsafe(?:HTML|SVG|MathML|Static)\b|dangerouslySetInnerHTML|DOMPurify`,
        String.raw`\beval\s*\(|\bFunction\s*\(|document\.write`,
        // Event handler attributes and script URLs in HTML or SVG markup
        String.raw`<[^>]*\son[a-z]+\s*=|javascript:`,
      ].join('|'),
      'u',
    ),
    // Reading or removing the property of an element in Java, such as `getProperty("innerHTML")`
    except: /\b(?:get|has|remove)Property\s*\(\s*"(?:inner|outer)HTML"\s*\)/u,
  },
  {
    category: 'red',
    reason: 'adds network requests',
    // A bare `fetch()`, not a `fetch` method such as `dataProvider.fetch()`
    pattern: new RegExp(
      [
        String.raw`(?:^|[^\w.]|\b(?:window|globalThis|self)\.)fetch\s*\(`,
        String.raw`XMLHttpRequest|withCredentials|sendBeacon|new\s+(?:WebSocket|EventSource)\b`,
        // A dynamic import of anything but a string literal
        String.raw`\bimport\s*\(\s*(?!['"])`,
      ].join('|'),
      'u',
    ),
    javaStringsOnly: true,
  },
  {
    category: 'red',
    reason: 'adds browser storage or cookies',
    pattern: /\b(?:localStorage|sessionStorage|indexedDB|cookieStore)\b|document\.cookie/u,
    javaStringsOnly: true,
    // Flow's dev server only runs in development mode, on the developer's machine
    exceptPaths: ['vaadin-dev-server/**'],
  },
];

/** A breaking change, marked with `!` in the conventional commit title */
export const BREAKING_TITLE = /^\w+(?:\([^)]*\))?!:/u;

/** A breaking change, marked with a conventional commit footer in the description */
export const BREAKING_BODY = /^BREAKING[ -]CHANGE:/mu;

/**
 * Authors whose pull requests can be green. Other authors need at least one
 * human approval. Private members of the organization show up as CONTRIBUTOR,
 * so the workflow also checks the membership. Bot accounts (GitHub Apps such
 * as the agents that open most pull requests) are trusted too: they can only
 * push to this repository when installed on it, and a bot pull request from a
 * fork still needs a human approval by the head branch rule.
 */
export const TRUSTED_ASSOCIATIONS = ['OWNER', 'MEMBER', 'COLLABORATOR'];
