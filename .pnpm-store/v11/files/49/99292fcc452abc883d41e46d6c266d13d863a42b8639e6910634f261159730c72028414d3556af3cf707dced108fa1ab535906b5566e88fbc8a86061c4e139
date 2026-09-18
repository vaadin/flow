/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

import * as assert from 'assert';
import * as fs from 'fs';
import * as jsonschema from 'jsonschema';
import * as path from 'path';
import * as logging from 'plylog';
import {applyBuildPreset, isValidPreset, ProjectBuildOptions} from './builds';
import minimatchAll = require('minimatch-all');
import {FsUrlLoader, PackageUrlResolver, WarningFilter, Analyzer, Severity} from 'polymer-analyzer';

export {ProjectBuildOptions, JsCompileTarget, applyBuildPreset} from './builds';

const logger = logging.getLogger('polymer-project-config');

/**
 * The default globs for matching all user application source files.
 */
export const defaultSourceGlobs = ['src/**/*'];

export type ModuleResolutionStrategy = 'none'|'node';
const moduleResolutionStrategies =
    new Set<ModuleResolutionStrategy>(['none', 'node']);

/**
 * Resolve any glob or path from the given path, even if glob
 * is negative (begins with '!').
 */
function globResolve(fromPath: string, glob: string): string {
  if (glob.startsWith('!')) {
    const includeGlob = glob.substring(1);
    return '!' + path.resolve(fromPath, includeGlob);
  } else {
    return path.resolve(fromPath, glob);
  }
}

/**
 * Returns a relative path for a glob or path, even if glob
 * is negative (begins with '!').
 */
function globRelative(fromPath: string, glob: string): string {
  if (glob.startsWith('!')) {
    return '!' + path.relative(fromPath, glob.substr(1));
  }
  return path.relative(fromPath, glob);
}

/**
 * Returns a positive glob, even if glob is negative (begins with '!')
 */
function getPositiveGlob(glob: string): string {
  if (glob.startsWith('!')) {
    return glob.substring(1);
  } else {
    return glob;
  }
}

/**
 * Given a user-provided options object, check for deprecated options. When one
 * is found, warn the user and fix if possible.
 */
// tslint:disable-next-line: no-any User input represented as any.
function fixDeprecatedOptions(options: any): ProjectOptions {
  if (typeof options.sourceGlobs !== 'undefined') {
    logger.warn(
        '"sourceGlobs" config option has been renamed to "sources" and will no longer be supported in future versions');
    options.sources = options.sources || options.sourceGlobs;
  }
  if (typeof options.includeDependencies !== 'undefined') {
    logger.warn(
        '"includeDependencies" config option has been renamed to "extraDependencies" and will no longer be supported in future versions');
    options.extraDependencies =
        options.extraDependencies || options.includeDependencies;
  }
  // TODO(rictic): two releases after v3.5.0, start warning about
  //     options.lint.ignoreWarnings. For now we'll start by just
  //     making them always point to the same object.
  if (options.lint && options.lint.warningsToIgnore) {
    options.lint.ignoreWarnings = options.lint.warningsToIgnore;
  } else if (options.lint && options.lint.ignoreWarnings) {
    options.lint.warningsToIgnore = options.lint.ignoreWarnings;
  }
  return options;
}

export interface LintOptions {
  /**
   * The lint rules to run. Can be the code of a collection of rules like
   * "polymer-2" or an individual rule like "dom-module-invalid-attrs".
   */
  rules: string[];

  /**
   * Warnings to ignore. After the rules are run, any warning that matches
   * one of these codes is ignored, project-wide.
   */
  warningsToIgnore?: string[];

  /**
   * Deprecated way of spelling the `warningsToIgnore` lint option.
   *
   * Used only if `warningsToIgnore` is not specified.
   */
  ignoreWarnings?: string[];

  /**
   * An array of file globs to never report warnings for.
   *
   * The globs follow [minimatch] syntax, and any file that matches any
   * of the listed globs will never show any linter warnings. This will
   * typically not have a performance benefit, as the file will usually
   * still need to be analyzed.
   *
   * [minimatch]: https://github.com/isaacs/minimatch
   */
  filesToIgnore?: string[];
}

export interface ProjectOptions {
  /**
   * Path to the root of the project on the filesystem. This can be an absolute
   * path, or a path relative to the current working directory. Defaults to the
   * current working directory of the process.
   */
  root?: string;

  /**
   * The path relative to `root` of the entrypoint file that will be served for
   * app-shell style projects. Usually this is index.html.
   */
  entrypoint?: string;

  /**
   * The path relative to `root` of the app shell element.
   */
  shell?: string;

  /**
   * The path relative to `root` of the lazily loaded fragments. Usually the
   * pages of an app or other bundles of on-demand resources.
   */
  fragments?: string[];

  /**
   * List of glob patterns, relative to root, of this project's sources to read
   * from the file system.
   */
  sources?: string[];

  /**
   * List of file paths, relative to the project directory, that should be
   * included as extraDependencies in the build target.
   */
  extraDependencies?: string[];

  /**
   * List of build option configurations.
   */
  builds?: ProjectBuildOptions[];

  /**
   * Set `basePath: true` on all builds. See that option for more details.
   */
  autoBasePath?: boolean;

  /**
   * Options for the Polymer Linter.
   */
  lint?: LintOptions;

  /**
   * Sets other options' defaults to NPM-appropriate values:
   *
   *   - 'componentDir': 'node_modules/'
   */
  npm?: boolean;

  /**
   * The directory containing this project's dependencies.
   */
  componentDir?: string;

  /**
   * Algorithm to use for resolving module specifiers in import and export
   * statements when rewriting them to be web-compatible. Valid values are:
   *
   * "none": Disable module specifier rewriting. This is the default.
   * "node": Use Node.js resolution to find modules.
   */
  moduleResolution?: ModuleResolutionStrategy;
}

export class ProjectConfig {
  readonly root: string;
  readonly entrypoint: string;
  readonly shell?: string;
  readonly fragments: string[];
  readonly sources: string[];
  readonly extraDependencies: string[];
  readonly componentDir?: string;
  readonly npm?: boolean;
  readonly moduleResolution: ModuleResolutionStrategy;

  readonly builds: ProjectBuildOptions[];
  readonly autoBasePath: boolean;
  readonly allFragments: string[];
  readonly lint: LintOptions|undefined = undefined;

  /**
   * Given an absolute file path to a polymer.json-like ProjectOptions object,
   * read that file. If no file exists, null is returned. If the file exists
   * but there is a problem reading or parsing it, throw an exception.
   *
   * TODO: in the next major version we should make this method and the one
   *     below async.
   */
  static loadOptionsFromFile(filepath: string): ProjectOptions|null {
    try {
      const configContent = fs.readFileSync(filepath, 'utf-8');
      const contents = JSON.parse(configContent);
      return this.validateOptions(contents);
    } catch (error) {
      // swallow "not found" errors because they are so common / expected
      if (error && error.code === 'ENOENT') {
        logger.debug('no polymer config file found', {file: filepath});
        return null;
      }
      // otherwise, throw an exception
      throw error;
    }
  }

  /**
   * Given an absolute file path to a polymer.json-like ProjectOptions object,
   * return a new ProjectConfig instance created with those options.
   */
  static loadConfigFromFile(filepath: string): ProjectConfig|null {
    const configParsed = ProjectConfig.loadOptionsFromFile(filepath);
    if (!configParsed) {
      return null;
    }
    return new ProjectConfig(configParsed);
  }

  /**
   * Returns the given configJsonObject if it is a valid ProjectOptions object,
   * otherwise throws an informative error message.
   */
  static validateOptions(configJsonObject: {}): ProjectOptions {
    const validator = new jsonschema.Validator();
    const result = validator.validate(configJsonObject, getSchema());
    if (result.errors.length > 0) {
      const error = result.errors[0]!;
      if (!error.property && !error.message) {
        throw error;
      }
      let propertyName = error.property;
      if (propertyName.startsWith('instance.')) {
        propertyName = propertyName.slice(9);
      }
      throw new Error(`Property '${propertyName}' ${error.message}`);
    }
    return configJsonObject;
  }

  /**
   * Returns a new ProjectConfig from the given JSON object if it's valid.
   *
   * TODO(rictic): For the next major version we should mark the constructor
   * private, or perhaps make it validating. Also, we should standardize the
   * naming scheme across the static methods on this class.
   *
   * Throws if the given JSON object is an invalid ProjectOptions.
   */
  static validateAndCreate(configJsonObject: {}) {
    const options = this.validateOptions(configJsonObject);
    return new this(options);
  }

  /**
   * Given a project directory, return an Analyzer (and related objects) with
   * configuration inferred from polymer.json (and possibly other config files
   * that we find and interpret).
   */
  static async initializeAnalyzerFromDirectory(dirname: string) {
    const config =
        this.loadConfigFromFile(path.join(dirname, 'polymer.json')) ||
        new ProjectConfig({root: dirname});
    return config.initializeAnalyzer();
  }

  /**
   * constructor - given a ProjectOptions object, create the correct project
   * configuration for those options. This involves setting the correct
   * defaults, validating options, warning on deprecated options, and
   * calculating some additional properties.
   */
  constructor(options: ProjectOptions) {
    options = (options) ? fixDeprecatedOptions(options) : {};

    /**
     * npm
     */
    this.npm = options.npm;

    // Set defaults for all NPM related options.
    if (this.npm) {
      this.componentDir = 'node_modules/';
    }

    /**
     * moduleResolution
     */
    this.moduleResolution = options.moduleResolution || 'node';

    /**
     * root
     */
    if (options.root) {
      this.root = path.resolve(options.root);
    } else {
      this.root = process.cwd();
    }

    /**
     * entrypoint
     */
    if (options.entrypoint) {
      this.entrypoint = path.resolve(this.root, options.entrypoint);
    } else {
      this.entrypoint = path.resolve(this.root, 'index.html');
    }

    /**
     * shell
     */
    if (options.shell) {
      this.shell = path.resolve(this.root, options.shell);
    }

    /**
     * fragments
     */
    if (options.fragments) {
      this.fragments = options.fragments.map((e) => path.resolve(this.root, e));
    } else {
      this.fragments = [];
    }

    /**
     * extraDependencies
     */
    this.extraDependencies = (options.extraDependencies ||
                              []).map((glob) => globResolve(this.root, glob));

    /**
     * sources
     */
    this.sources = (options.sources || defaultSourceGlobs)
                       .map((glob) => globResolve(this.root, glob));
    this.sources.push(this.entrypoint);
    if (this.shell) {
      this.sources.push(this.shell);
    }
    if (this.fragments) {
      this.sources = this.sources.concat(this.fragments);
    }

    /**
     * allFragments
     */
    this.allFragments = [];
    // It's important that shell is first for document-ordering of imports
    if (this.shell) {
      this.allFragments.push(this.shell);
    }
    if (this.fragments) {
      this.allFragments = this.allFragments.concat(this.fragments);
    }
    if (this.allFragments.length === 0) {
      this.allFragments.push(this.entrypoint);
    }

    if (options.lint) {
      this.lint = options.lint;
    }

    /**
     * builds
     */
    if (options.builds) {
      this.builds = options.builds;
      if (Array.isArray(this.builds)) {
        this.builds = this.builds.map(applyBuildPreset);
      }
    }

    /**
     * autoBasePath
     */
    if (options.autoBasePath) {
      this.autoBasePath = options.autoBasePath;

      for (const build of this.builds || []) {
        build.basePath = true;
      }
    }

    /**
     * componentDir
     */
    if (options.componentDir) {
      this.componentDir = options.componentDir;
    }
  }

  /**
   * Get an analyzer (and other related objects) with configuration determined
   * by this ProjectConfig.
   */
  async initializeAnalyzer() {
    const urlLoader = new FsUrlLoader(this.root);
    const urlResolver = new PackageUrlResolver(
        {packageDir: this.root, componentDir: this.componentDir});

    const analyzer = new Analyzer({
      urlLoader,
      urlResolver,
      moduleResolution: convertModuleResolution(this.moduleResolution)
    });
    const lintConfig: Partial<LintOptions> = this.lint || {};
    const warningFilter = new WarningFilter({
      minimumSeverity: Severity.WARNING,
      warningCodesToIgnore: new Set(lintConfig.warningsToIgnore || []),
      filesToIgnore: lintConfig.filesToIgnore || []
    });
    return {urlLoader, urlResolver, analyzer, warningFilter};
  }

  isFragment(filepath: string): boolean {
    return this.allFragments.indexOf(filepath) !== -1;
  }

  isShell(filepath: string): boolean {
    return (!!this.shell && (this.shell === filepath));
  }

  isSource(filepath: string): boolean {
    return minimatchAll(filepath, this.sources);
  }

  /**
   * Validates that a configuration is accurate, and that all paths are
   * contained within the project root.
   */
  validate(): boolean {
    const validateErrorPrefix = `Polymer Config Error`;
    if (this.entrypoint) {
      assert(
          this.entrypoint.startsWith(this.root),
          `${validateErrorPrefix}: entrypoint (${this.entrypoint}) ` +
              `does not resolve within root (${this.root})`);
    }
    if (this.shell) {
      assert(
          this.shell.startsWith(this.root),
          `${validateErrorPrefix}: shell (${this.shell}) ` +
              `does not resolve within root (${this.root})`);
    }
    this.fragments.forEach((f) => {
      assert(
          f.startsWith(this.root),
          `${validateErrorPrefix}: a "fragments" path (${f}) ` +
              `does not resolve within root (${this.root})`);
    });
    this.sources.forEach((s) => {
      assert(
          getPositiveGlob(s).startsWith(this.root),
          `${validateErrorPrefix}: a "sources" path (${s}) ` +
              `does not resolve within root (${this.root})`);
    });
    this.extraDependencies.forEach((d) => {
      assert(
          getPositiveGlob(d).startsWith(this.root),
          `${validateErrorPrefix}: an "extraDependencies" path (${d}) ` +
              `does not resolve within root (${this.root})`);
    });
    assert(
        moduleResolutionStrategies.has(this.moduleResolution),
        `${validateErrorPrefix}: "moduleResolution" must be one of: ` +
            `${[...moduleResolutionStrategies].join(', ')}.`);

    // TODO(fks) 11-14-2016: Validate that files actually exist in the
    // file system. Potentially become async function for this.

    if (this.builds) {
      assert(
          Array.isArray(this.builds),
          `${validateErrorPrefix}: "builds" (${this.builds}) ` +
              `expected an array of build configurations.`);

      if (this.builds.length > 1) {
        const buildNames = new Set<string>();
        for (const build of this.builds) {
          const buildName = build.name;
          const buildPreset = build.preset;
          assert(
              !buildPreset || isValidPreset(buildPreset),
              `${validateErrorPrefix}: "${buildPreset}" is not a valid ` +
                  ` "builds" preset.`);
          assert(
              buildName,
              `${validateErrorPrefix}: all "builds" require ` +
                  `a "name" property when there are multiple builds defined.`);
          assert(
              !buildNames.has(buildName!),
              `${validateErrorPrefix}: "builds" duplicate build name ` +
                  `"${buildName}" found. Build names must be unique.`);
          buildNames.add(buildName!);
        }
      }
    }

    return true;
  }

  /**
   * Generate a JSON string serialization of this configuration. File paths
   * will be relative to root.
   */
  toJSON(): string {
    let lintObj = undefined;
    if (this.lint) {
      lintObj = {...this.lint};
      delete lintObj.ignoreWarnings;
    }
    const isWindows = process.platform === 'win32';
    const normalizePath = isWindows ?
        (path: string) => path.replace(/\\/g, '/') :
        (path: string) => path;
    const obj = {
      entrypoint: globRelative(this.root, this.entrypoint),
      shell: this.shell ? globRelative(this.root, this.shell) : undefined,
      fragments: this.fragments.map((absolutePath) => {
        return normalizePath(globRelative(this.root, absolutePath));
      }),
      sources: this.sources.map((absolutePath) => {
        return normalizePath(globRelative(this.root, absolutePath));
      }),
      extraDependencies: this.extraDependencies.map((absolutePath) => {
        return normalizePath(globRelative(this.root, absolutePath));
      }),
      builds: this.builds,
      autoBasePath: this.autoBasePath,
      lint: lintObj,
      npm: this.npm,
      componentDir: this.componentDir,
      moduleResolution: this.moduleResolution,
    };
    return JSON.stringify(obj, null, 2);
  }
}

// Gets the json schema for polymer.json, generated from the typescript
// interface for runtime validation. See the build script in package.json for
// more info.
const getSchema: () => jsonschema.Schema = (() => {
  let schema: jsonschema.Schema;

  return () => {
    if (schema === undefined) {
      schema = JSON.parse(
          fs.readFileSync(path.join(__dirname, 'schema.json'), 'utf-8'));
    }
    return schema;
  };
})();


/**
 * Module resolution in ProjectConfig is different than the same-named parameter
 * in the analyzer. So we need to convert between the two.
 */
function convertModuleResolution(moduleResolution: 'node'|'none'): 'node'|
    undefined {
  switch (moduleResolution) {
    case 'node':
      return 'node';
    case 'none':
      return undefined;
    default:
      const never: never = moduleResolution;
      throw new Error(`Unknown module resolution parameter: ${never}`);
  }
}
