#!/usr/bin/env node
/**
 * Computes the semantic versioning level of the API changes a pull request
 * makes, for the +0.0.1 / +0.1.0 / +1.0.0 label of the api-diff-labeling job.
 *
 * The API is compared against the latest release of the target branch, the
 * reference version, and not against the target branch itself, so that a
 * change to API that was never released does not count as a breaking change.
 * Comparing only the pull request against the release would label it with
 * every change merged since that release, though, so both the merge base and
 * the pull request are compared against the release, and only the changes
 * found for the pull request and not for the merge base count.
 *
 * The reference version is the highest stable, beta or rc release of Flow on
 * Maven Central whose major.minor version is not above the one of the target
 * branch: the latest patch release on a maintenance branch, the latest beta or
 * rc of the version under development on main, or the latest release of the
 * previous minor version while main only has alphas. Alphas are not on Maven
 * Central and their API is not stable enough to compare against.
 *
 * Usage:
 *   node scripts/apiDiff.js modules
 *     Prints the comma separated modules to compare, for `mvn -pl`.
 *   node scripts/apiDiff.js reference-version --branch-version=<version>
 *     Prints the reference version for a branch at the given version.
 *   node scripts/apiDiff.js compare --reference=<version> --base-dir=<dir>
 *       --base-version=<version> --head-version=<version> --out=<dir>
 *     Compares the jars built in <base-dir> (the merge base) and in the
 *     current directory (the pull request) against the reference version.
 *     Writes the japicmp reports and summary.md to <out> and appends
 *     `level=<x.y.z>` to $GITHUB_OUTPUT (printed if not set).
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawnSync } = require('child_process');
const { parseArgs } = require('util');

const MAVEN_CENTRAL = 'https://repo1.maven.org/maven2';
const JAPICMP_VERSION = '0.22.0';
// The module whose releases decide the reference version. Every Flow release
// publishes it.
const REFERENCE_ARTIFACT = 'flow-server';

const PATCH = '0.0.1';
const MINOR = '0.1.0';
const MAJOR = '1.0.0';

function readSource(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null;
}

/**
 * Reads a tag of the project itself, not of its parent, its dependencies or
 * anything else the pom nests.
 */
function readProjectValue(pom, tag) {
  const own = pom.replace(
    /<(parent|properties|dependencyManagement|dependencies|build|profiles|reporting|repositories|pluginRepositories)>[\s\S]*?<\/\1>/g,
    ''
  );
  return own.match(new RegExp(`<${tag}>([^<]*)</${tag}>`))?.[1];
}

/**
 * Top-level modules that produce a jar and do not opt out with the
 * flow.apicmp.skip property.
 */
function findComparedModules(readSource) {
  const root = readSource('pom.xml');
  return [...root.matchAll(/<module>([^<]*)<\/module>/g)]
    .map((match) => match[1])
    .filter((module) => {
      const pom = readSource(`${module}/pom.xml`);
      return (
        pom !== null &&
        !/<packaging>pom<\/packaging>/.test(pom) &&
        !/<flow\.apicmp\.skip>true<\/flow\.apicmp\.skip>/.test(pom)
      );
    });
}

/** Sort key of a stable, beta or rc version, or null for any other version. */
function parseReleaseVersion(version) {
  const match = version.match(/^(\d+)\.(\d+)\.(\d+)(?:-(beta|rc)(\d+))?$/);
  if (!match) {
    return null;
  }
  // A beta sorts before an rc, and both before the release
  const stage = ['beta', 'rc', undefined].indexOf(match[4]);
  return [+match[1], +match[2], +match[3], stage, +(match[5] || 0)];
}

function compareKeys(a, b) {
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return a[i] - b[i];
    }
  }
  return 0;
}

/** Picks the reference version among the published versions, see the top of the file. */
function selectReferenceVersion(branchVersion, publishedVersions) {
  const [major, minor] = branchVersion.split(/[.-]/).map(Number);
  const candidates = publishedVersions
    .map((version) => ({ version, key: parseReleaseVersion(version) }))
    .filter(({ key }) => key && compareKeys(key.slice(0, 2), [major, minor]) <= 0)
    .sort((a, b) => compareKeys(a.key, b.key));
  if (!candidates.length) {
    throw new Error(`No release found for branch version ${branchVersion}`);
  }
  return candidates.at(-1).version;
}

function decodeEntities(value) {
  return value.replace(
    /&(lt|gt|quot|apos|amp);/g,
    (_, name) => ({ lt: '<', gt: '>', quot: '"', apos: "'", amp: '&' })[name]
  );
}

/** Parses the elements and attributes of an XML document; japicmp reports have no text content. */
function parseXml(xml) {
  const root = { tag: '#document', attrs: {}, children: [] };
  const stack = [root];
  for (const [, closing, tag, attrText, selfClosing] of xml.matchAll(/<(\/?)([A-Za-z][\w.-]*)([^>]*?)(\/?)>/g)) {
    if (closing) {
      stack.pop();
      continue;
    }
    const attrs = {};
    for (const [, name, value] of attrText.matchAll(/([\w:.-]+)="([^"]*)"/g)) {
      attrs[name] = decodeEntities(value);
    }
    const node = { tag, attrs, children: [] };
    stack.at(-1).children.push(node);
    if (!selfClosing) {
      stack.push(node);
    }
  }
  return root;
}

function listChildren(node, tag) {
  return node.children.filter((child) => child.tag === tag).flatMap((child) => child.children);
}

/** Names the element a change is reported for, or returns null for an element that is part of its owner. */
function describeElement(node, owner) {
  const parameters = () =>
    listChildren(node, 'parameters')
      .map((parameter) => parameter.attrs.type)
      .join(', ');
  switch (node.tag) {
    case 'class':
      return node.attrs.fullyQualifiedName;
    case 'method':
      return `${owner}#${node.attrs.name}(${parameters()})`;
    case 'constructor':
      return `${owner}#<init>(${parameters()})`;
    case 'field':
      return `${owner}#${node.attrs.name}`;
    case 'interface':
      return `${owner} implements ${node.attrs.fullyQualifiedName}`;
    case 'superclass':
      return `${owner} superclass`;
    case 'annotation':
      return `@${node.attrs.fullyQualifiedName} on ${owner}`;
    default:
      return null;
  }
}

const STATUS_TAGS = new Set(['class', 'method', 'constructor', 'field', 'interface', 'superclass']);

/**
 * Reads the level japicmp gives each type of compatibility change, which the
 * XML report does not include, with ChangeTypeLevels.java.
 */
function readChangeTypeLevels(japicmp) {
  const result = spawnSync('java', ['-cp', japicmp, path.join(__dirname, 'ChangeTypeLevels.java')], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'inherit']
  });
  if (result.status !== 0) {
    throw new Error('Reading the japicmp compatibility change levels failed');
  }
  const levels = { PATCH, MINOR, MAJOR };
  return new Map(
    result.stdout
      .trim()
      .split('\n')
      .map((line) => {
        const [type, level] = line.split('=');
        if (!levels[level]) {
          throw new Error(`Unknown semantic version level in "${line}"`);
        }
        return [type, levels[level]];
      })
  );
}

/**
 * Lists the changes of a japicmp XML report as a map from a description of the
 * change to its level. A description names the element and the kind of the
 * change, but not the old and new values, so that a change to an element that
 * the merge base already changed since the release is not counted again.
 *
 * The levels are the ones japicmp uses for its semantic version: each type of
 * compatibility change has the level that changeTypeLevels gives it, and a
 * change without a compatibility change, like a new class or field, is a patch.
 */
function collectChanges(xml, changeTypeLevels) {
  const findLevel = (type) => {
    const level = changeTypeLevels.get(type);
    if (!level) {
      throw new Error(`No level known for the japicmp compatibility change ${type}`);
    }
    return level;
  };
  const changes = new Map();
  const add = (description, level) => {
    if (changes.get(description) !== MAJOR) {
      changes.set(description, level);
    }
  };
  const visit = (node, owner) => {
    const element = describeElement(node, owner) || owner;
    const status = node.attrs.changeStatus;
    const compatibilityChanges = listChildren(node, 'compatibilityChanges');
    compatibilityChanges.forEach((change) => add(`${element}: ${change.attrs.type}`, findLevel(change.attrs.type)));
    // Some changes, like a new field, have no compatibility change. A
    // modified class only reports that one of its elements changed.
    if (
      STATUS_TAGS.has(node.tag) &&
      !compatibilityChanges.length &&
      (status === 'NEW' || status === 'REMOVED' || (status === 'MODIFIED' && node.tag !== 'class'))
    ) {
      add(`${element}: ${status}`, PATCH);
    }
    // A new or removed element is one change, whatever it contains
    if (status !== 'NEW' && status !== 'REMOVED') {
      node.children.forEach((child) => visit(child, element));
    }
  };
  visit(parseXml(xml), null);
  return changes;
}

function findHighestLevel(levels) {
  return [...levels].reduce((highest, level) => (level > highest ? level : highest), PATCH);
}

/** The changes found against the reference for the pull request and not for the merge base. */
function findPullRequestChanges(baseChanges, headChanges) {
  return new Map([...headChanges].filter(([description]) => !baseChanges.has(description)));
}

async function download(url, file) {
  if (fs.existsSync(file)) {
    return file;
  }
  const response = await fetch(url);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Downloading ${url} failed with HTTP ${response.status}`);
  }
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, Buffer.from(await response.arrayBuffer()));
  return file;
}

function buildArtifactUrl(groupId, artifactId, version, classifier) {
  const suffix = classifier ? `-${classifier}` : '';
  return `${MAVEN_CENTRAL}/${groupId.replace(/\./g, '/')}/${artifactId}/${version}/${artifactId}-${version}${suffix}.jar`;
}

function runJapicmp(japicmp, changeTypeLevels, oldJar, newJar, xmlFile, htmlFile) {
  const args = ['-jar', japicmp, '-o', oldJar, '-n', newJar, '-m', '--ignore-missing-classes', '-x', xmlFile];
  if (htmlFile) {
    args.push('--html-file', htmlFile);
  }
  // The text report on standard output repeats the XML report
  const result = spawnSync('java', args, { stdio: ['ignore', 'ignore', 'inherit'] });
  if (result.status !== 0) {
    throw new Error(`japicmp failed comparing ${newJar} against ${oldJar}`);
  }
  return collectChanges(fs.readFileSync(xmlFile, 'utf8'), changeTypeLevels);
}

function formatSummary(reference, results, level) {
  const lines = [`### API changes against ${reference}: +${level}`, ''];
  for (const { module, note, added, reverted } of results) {
    if (!note && !added.size && !reverted.size) {
      continue;
    }
    lines.push(`#### ${module}`, '');
    if (note) {
      lines.push(note, '');
    }
    if (added.size) {
      lines.push('Changes this pull request makes:', '');
      [...added].sort().forEach(([description, changeLevel]) => lines.push(`- \`${description}\` (+${changeLevel})`));
      lines.push('');
    }
    if (reverted.size) {
      lines.push(`Changes merged after ${reference} that this pull request takes back, not counted:`, '');
      [...reverted].sort().forEach(([description]) => lines.push(`- \`${description}\``));
      lines.push('');
    }
  }
  if (lines.length === 2) {
    lines.push('No API changes.');
  }
  return lines.join('\n') + '\n';
}

async function compare(options) {
  const out = path.resolve(options.out);
  // Kept out of the reports, which the workflow uploads
  const cache = path.join(os.tmpdir(), 'flow-api-diff');
  const japicmp = await download(
    buildArtifactUrl('com.github.siom79.japicmp', 'japicmp', JAPICMP_VERSION, 'jar-with-dependencies'),
    path.join(cache, `japicmp-${JAPICMP_VERSION}.jar`)
  );
  const changeTypeLevels = readChangeTypeLevels(japicmp);

  const results = [];
  for (const module of findComparedModules(readSource)) {
    const pom = readSource(`${module}/pom.xml`);
    const artifactId = readProjectValue(pom, 'artifactId');
    // A project without a groupId of its own inherits the one of its parent
    const groupId = readProjectValue(pom, 'groupId') || pom.match(/<parent>[\s\S]*?<groupId>([^<]*)</)[1];
    const findJar = (dir, version) => {
      const file = path.join(dir, module, 'target', `${artifactId}-${version}.jar`);
      return fs.existsSync(file) ? file : null;
    };
    const headJar = findJar('.', options['head-version']);
    if (!headJar) {
      throw new Error(`${module} has not been built for version ${options['head-version']}`);
    }
    const baseJar = findJar(options['base-dir'], options['base-version']);
    // A module added after the reference release is compared against the merge base
    const referenceJar =
      (await download(
        buildArtifactUrl(groupId, artifactId, options.reference),
        path.join(cache, `${artifactId}-${options.reference}.jar`)
      )) || baseJar;
    const reportDir = path.join(out, module);
    fs.mkdirSync(reportDir, { recursive: true });

    if (!referenceJar) {
      results.push({ module, note: 'New module.', added: new Map([[`${module}: NEW`, MINOR]]), reverted: new Map() });
      continue;
    }
    const baseChanges = baseJar
      ? runJapicmp(japicmp, changeTypeLevels, referenceJar, baseJar, path.join(reportDir, 'base.xml'))
      : new Map();
    const headChanges = runJapicmp(
      japicmp,
      changeTypeLevels,
      referenceJar,
      headJar,
      path.join(reportDir, 'japicmp.xml'),
      path.join(reportDir, 'japicmp.html')
    );
    results.push({
      module,
      note: referenceJar === baseJar ? `Not released in ${options.reference}, compared against the merge base.` : null,
      added: findPullRequestChanges(baseChanges, headChanges),
      reverted: findPullRequestChanges(headChanges, baseChanges)
    });
  }

  const level = findHighestLevel(results.flatMap(({ added }) => [...added.values()]));
  const summary = formatSummary(options.reference, results, level);
  fs.writeFileSync(path.join(out, 'summary.md'), summary);
  console.log(summary);
  const output = `level=${level}\n`;
  if (process.env.GITHUB_OUTPUT) {
    fs.appendFileSync(process.env.GITHUB_OUTPUT, output);
  } else {
    process.stdout.write(output);
  }
}

async function printReferenceVersion(branchVersion) {
  const url = `${MAVEN_CENTRAL}/com/vaadin/${REFERENCE_ARTIFACT}/maven-metadata.xml`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Downloading ${url} failed with HTTP ${response.status}`);
  }
  const versions = [...(await response.text()).matchAll(/<version>([^<]*)<\/version>/g)].map((match) => match[1]);
  console.log(selectReferenceVersion(branchVersion, versions));
}

async function main() {
  const { positionals, values } = parseArgs({
    allowPositionals: true,
    options: Object.fromEntries(
      ['branch-version', 'reference', 'base-dir', 'base-version', 'head-version', 'out'].map((name) => [
        name,
        { type: 'string' }
      ])
    )
  });
  switch (positionals[0]) {
    case 'modules':
      console.log(findComparedModules(readSource).join(','));
      break;
    case 'reference-version':
      await printReferenceVersion(values['branch-version']);
      break;
    case 'compare':
      await compare(values);
      break;
    default:
      console.error('Usage: node scripts/apiDiff.js modules | reference-version | compare, see the top of the file');
      process.exit(1);
  }
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message);
    process.exit(1);
  });
}

module.exports = {
  findComparedModules,
  selectReferenceVersion,
  collectChanges,
  findPullRequestChanges,
  findHighestLevel
};
