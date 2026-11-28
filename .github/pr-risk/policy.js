/**
 * The policy of the PR risk workflow (.github/workflows/pr-risk.yml). Every
 * rule that matches a pull request sets a minimum level for it. The agent can
 * suggest a higher level than the policy, never a lower one.
 *
 * The workflow runs the policy of the base branch, so a pull request that edits
 * this file is classified by the policy it replaces.
 */

/**
 * Path rules match when one of their globs matches the path of a changed file,
 * or the previous path of a renamed file, unless `exceptPaths` matches all of
 * its paths. Wildcards also match names that start with a dot. A rule with
 * `codeOnly` skips files whose changes are only comments and blank lines. A
 * rule with `removed` sets its lower level for removed files that match its
 * paths. A rule with `checksOnly` sets its lower level for files that match
 * its paths, but not its `exceptPaths`, when no changed line matches
 * `sensitive`. A rule with `exceptPatchBumps` skips a PATCH_BUMPS pull request.
 */
export const PATH_RULES = [
  {
    level: 'high',
    reason: 'CI, release and repository automation, or config that runs code',
    paths: [
      '.github/**',
      '.husky/**',
      '**/.npmrc',
      '**/.gitattributes',
      'scripts/**',
      'build-tools/**',
      'release-check/**',
      'flow-bom/**',
      '.mvn/**',
    ],
    // Documentation that no workflow reads
    exceptPaths: [
      '.github/workflows/README.md',
      '.github/RELEASE_DIGEST.md',
      '.github/ISSUE_TEMPLATE.md',
      '.github/ISSUE_TEMPLATE/**',
    ],
    // A removed workflow runs nothing, but CI may rely on it
    removed: {
      level: 'medium',
      reason: 'removes CI workflows',
      paths: ['.github/workflows/**'],
    },
    // Workflows and scripts that only check pull requests cannot reach a
    // secret or a release unless they change the lines that grant that power.
    // The merge gates (PR risk, the API diff that sets the `+0.0.1` label,
    // auto-merge), the agent workflows and the scripts that push with a token
    // stay high.
    checksOnly: {
      level: 'medium',
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
          // The API diff and auto-merge steps
          String.raw`\+0\.0\.1|\+0\.1\.0|\+1\.0\.0|version_change|api-?[dD]iff|apicmp|japicmp|auto-?[mM]erge`,
          // Text from the pull request inserted into a script
          String.raw`\$\{\{\s*(?:github\.event|github\.head_ref|inputs)`,
        ].join('|'),
        'mu',
      ),
    },
  },
  // The security rules skip Javadoc changes, such as a new `@since` tag
  {
    level: 'high',
    reason: 'access control, authentication and CSRF protection',
    codeOnly: true,
    paths: [
      'flow-server/src/main/java/com/vaadin/flow/server/auth/**',
      'flow-server/src/main/java/com/vaadin/flow/internal/springcsrf/**',
      'vaadin-spring/src/main/java/com/vaadin/flow/spring/security/**',
    ],
  },
  {
    level: 'high',
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
    level: 'high',
    reason: 'file uploads to the server',
    codeOnly: true,
    paths: [
      'flow-server/src/main/java/com/vaadin/flow/server/communication/StreamReceiverHandler.java',
      'flow-server/src/main/java/com/vaadin/flow/server/communication/TransferUtil.java',
      'flow-server/src/main/java/com/vaadin/flow/server/streams/*Upload*.java',
    ],
  },
  {
    level: 'high',
    reason: 'usage statistics',
    codeOnly: true,
    paths: [
      'flow-server/src/main/java/com/vaadin/flow/internal/UsageStatistics*.java',
      'flow-server/src/main/java/com/vaadin/flow/server/UsageStatisticsCollector.java',
      'vaadin-dev-server/src/main/java/com/vaadin/base/devserver/stats/**',
    ],
  },
  {
    level: 'high',
    reason: 'licensing',
    codeOnly: true,
    paths: ['**/src/main/**/*LicenseChecker*'],
  },
  {
    level: 'medium',
    reason: 'dependencies',
    exceptPatchBumps: true,
    paths: ['**/package.json', '**/package-lock.json', '**/pnpm-lock.yaml', '**/pom.xml', '**/*.gradle*'],
    // Integration test modules, which no published artifact contains
    exceptPaths: ['flow-tests/**'],
  },
  {
    level: 'medium',
    reason: 'instructions that agents follow, including the agent review',
    paths: ['**/CLAUDE.md', '**/AGENTS.md', '**/.claude/**', '.mcp.json', 'CONVENTIONS.md', 'guidelines/**'],
  },
  {
    level: 'medium',
    reason: 'test, lint and build tooling that CI relies on',
    paths: [
      '*.config.{js,mjs,cjs,ts}',
      '**/tsconfig*.json',
      '.prettierrc*',
      'checkstyle/**',
      'eclipse/**',
      'flow-test-generic/**',
      'flow-test-util/**',
    ],
  },
  {
    level: 'medium',
    reason: 'core framework code that every application runs',
    codeOnly: true,
    paths: [
      'flow-server/src/main/**',
      'flow-client/src/main/**',
      'flow-data/src/main/**',
      'signals/src/main/**',
      'flow-push/src/main/**',
    ],
  },
  {
    level: 'medium',
    reason: 'Spring, CDI and Quarkus integration that every application on that stack runs',
    codeOnly: true,
    paths: ['vaadin-spring/src/main/**', 'vaadin-cdi/src/main/**', 'vaadin-quarkus/*/src/main/**'],
  },
  {
    level: 'medium',
    reason: 'build plugins and dev tooling that every application build runs',
    codeOnly: true,
    paths: [
      'flow-plugins/*/src/main/**',
      'flow-build-tools/src/main/**',
      'vaadin-dev-server/src/main/**',
      'flow-devloop-daemon/src/main/**',
      'flow-server-production-mode/src/main/**',
    ],
  },
];

/** Shipped code, which the content rules check: the files that match SHIPPED_PATHS but not TEST_PATHS */
export const SHIPPED_PATHS = ['**/src/main/**'];
export const TEST_PATHS = [
  'flow-tests/**',
  '**/src/test/**',
  '**/src/it/**',
  'flow-test-generic/**',
  'flow-test-util/**',
  'flow-html-components-testbench/**',
];

/**
 * Rules on the lines that a pull request adds to shipped code. Java lines that
 * only hold a comment are skipped, and so are lines that match `except` and
 * files that match `exceptPaths`. With `javaStringsOnly`, Java lines are only
 * checked in their string literals: Java has no such API of its own, so it
 * only matters in JavaScript that Java sends to the browser, such as
 * `executeJs("fetch(...)")`.
 */
export const CONTENT_RULES = [
  {
    level: 'high',
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
    level: 'high',
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
    level: 'high',
    reason: 'adds browser storage or cookies',
    pattern: /\b(?:localStorage|sessionStorage|indexedDB|cookieStore)\b|document\.cookie/u,
    javaStringsOnly: true,
    // The dev server only runs in development mode, on the developer's machine
    exceptPaths: ['vaadin-dev-server/**'],
  },
];

export const ISSUE_LABEL_RULES = [
  {
    label: 'Impact: High',
    level: 'medium',
    reason: 'a linked issue is labelled `Impact: High`: the problem reaches many applications',
  },
];

/**
 * Dependabot names a dependency `deps-dev` when no shipped artifact uses it,
 * such as a test or build plugin. A patch update of one that changes only the
 * version skips the rules with `exceptPatchBumps`.
 */
export const PATCH_BUMPS = {
  reason: 'a patch update of a build or test tool by Dependabot',
  authors: ['dependabot[bot]'],
  title: /^chore\(deps-dev\): bump \S+ from (\d+)\.(\d+)\.(\d+) to (\d+)\.(\d+)\.(\d+)$/u,
};

/** A breaking change, marked with `!` in the conventional commit title */
export const BREAKING_TITLE = /^\w+(?:\([^)]*\))?!:/u;

/** A breaking change, marked with a conventional commit footer in the description */
export const BREAKING_BODY = /^BREAKING[ -]CHANGE:/mu;

/**
 * Authors whose pull requests can be low. Private members of the organization
 * show up as CONTRIBUTOR, so the workflow also checks the membership. Bot
 * accounts are trusted too: they can only push here when installed, and a bot
 * pull request from a fork is still medium by the head branch rule.
 */
export const TRUSTED_ASSOCIATIONS = ['OWNER', 'MEMBER', 'COLLABORATOR'];
