/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

import * as babel from '@babel/types';
import * as jsdoc from 'doctrine';
import * as fsExtra from 'fs-extra';
import * as minimatch from 'minimatch';
import * as path from 'path';
import * as analyzer from 'polymer-analyzer';
import {Function as AnalyzerFunction} from 'polymer-analyzer/lib/javascript/function';
import Uri from 'vscode-uri';

import {closureParamToTypeScript, closureTypeToTypeScript} from './closure-types';
import {isEsModuleDocument, resolveImportExportFeature} from './es-modules';
import * as ts from './ts-ast';

/**
 * Configuration for declaration generation.
 */
export interface Config {
  /**
   * Skip source files whose paths match any of these glob patterns. If
   * undefined, defaults to excluding "index.html" and directories ending in
   * "test" or "demo".
   */
  excludeFiles?: string[];

  /**
   * The same as `excludeFiles`, for backwards compatibility. Will be removed in
   * next major version.
   */
  exclude?: string[];

  /**
   * Do not emit any declarations for features that have any of these
   * identifiers.
   */
  excludeIdentifiers?: string[];

  /**
   * Remove any triple-slash references to these files, specified as paths
   * relative to the analysis root directory.
   */
  removeReferences?: string[];

  /**
   * Additional files to insert as triple-slash reference statements. Given the
   * map `a: b[]`, a will get an additional reference statement for each file
   * path in b. All paths are relative to the analysis root directory.
   */
  addReferences?: {[filepath: string]: string[]};

  /**
   * Whenever a type with a name in this map is encountered, replace it with
   * the given name. Note this only applies to named types found in places like
   * function/method parameters and return types. It does not currently rename
   * e.g. entire generated classes.
   */
  renameTypes?: {[name: string]: string};

  /**
   * A map from an ES module path (relative to the analysis root directory) to
   * an array of identifiers exported by that module. If any of those
   * identifiers are encountered in a generated typings file, an import for that
   * identifier from the specified module will be inserted into the typings
   * file.
   */
  autoImport?: {[modulePath: string]: string[]};

  /**
   * If true, outputs declarations in 'goog:' modules instead of using
   * simple ES modules. This is a temporary hack to account for how modules are
   * resolved for TypeScript inside google3. This is probably not at all useful
   * for anyone but the Polymer team.
   */
  googModules?: boolean;

  /**
   * If true, does not log warnings detected when analyzing code,
   * only diagnostics of Error severity.
   */
  hideWarnings?: boolean;
}

const defaultExclude = [
  'index.html',
  'test/**',
  'demo/**',
];

/**
 * Analyze all files in the given directory using Polymer Analyzer, and return
 * TypeScript declaration document strings in a map keyed by relative path.
 */
export async function generateDeclarations(
    rootDir: string, config: Config): Promise<Map<string, string>> {
  // Note that many Bower projects also have a node_modules/, but the reverse is
  // unlikely.
  const isBowerProject =
      await fsExtra.pathExists(path.join(rootDir, 'bower_components')) === true;
  const a = new analyzer.Analyzer({
    urlLoader: new analyzer.FsUrlLoader(rootDir),
    urlResolver: new analyzer.PackageUrlResolver({
      packageDir: rootDir,
      componentDir: isBowerProject ? 'bower_components/' : 'node_modules/',
    }),
    moduleResolution: isBowerProject ? undefined : 'node',
  });
  const analysis = await a.analyzePackage();
  const outFiles = new Map<string, string>();
  for (const tsDoc of await analyzerToAst(analysis, config, rootDir)) {
    outFiles.set(tsDoc.path, tsDoc.serialize());
  }
  return outFiles;
}

/**
 * Make TypeScript declaration documents from the given Polymer Analyzer
 * result.
 */
async function analyzerToAst(
    analysis: analyzer.Analysis, config: Config, rootDir: string):
    Promise<ts.Document[]> {
  const excludeFiles = (config.excludeFiles || config.exclude || defaultExclude)
                           .map((p) => new minimatch.Minimatch(p));
  const addReferences = config.addReferences || {};
  const removeReferencesResolved = new Set(
      (config.removeReferences || []).map((r) => path.resolve(rootDir, r)));
  const renameTypes = new Map(Object.entries(config.renameTypes || {}));

  // Map from identifier to the module path that exports it.
  const autoImportMap = new Map<string, string>();
  if (config.autoImport !== undefined) {
    for (const importPath in config.autoImport) {
      for (const identifier of config.autoImport[importPath]) {
        autoImportMap.set(identifier, importPath);
      }
    }
  }

  const analyzerDocs = [
    ...analysis.getFeatures({kind: 'html-document'}),
    ...analysis.getFeatures({kind: 'js-document'}),
  ];

  // We want to produce one declarations file for each file basename. There
  // might be both `foo.html` and `foo.js`, and we want their declarations to be
  // combined into a signal `foo.d.ts`. So we first group Analyzer documents by
  // their declarations filename.
  const declarationDocs = new Map<string, analyzer.Document[]>();
  for (const jsDoc of analyzerDocs) {
    // For every HTML or JS file, Analyzer is going to give us 1) the top-level
    // document, and 2) N inline documents for any nested content (e.g. script
    // tags in HTML). The top-level document will give us all the nested
    // features we need, so skip any inline ones.
    if (jsDoc.isInline) {
      continue;
    }
    const sourcePath = analyzerUrlToRelativePath(jsDoc.url, rootDir);
    if (sourcePath === undefined) {
      console.warn(
          `Skipping source document without local file URL: ${jsDoc.url}`);
      continue;
    }
    if (excludeFiles.some((r) => r.match(sourcePath))) {
      continue;
    }
    const filename = makeDeclarationsFilename(sourcePath);
    let docs = declarationDocs.get(filename);
    if (!docs) {
      docs = [];
      declarationDocs.set(filename, docs);
    }
    docs.push(jsDoc);
  }

  const tsDocs = [];
  const warnings = [...analysis.getWarnings()];
  for (const [declarationsFilename, analyzerDocs] of declarationDocs) {
    const tsDoc = new ts.Document({
      path: declarationsFilename,
      header: makeHeader(
          analyzerDocs.map((d) => analyzerUrlToRelativePath(d.url, rootDir))
              .filter((url): url is string => url !== undefined)),
      tsLintDisables: [{
        ruleName: 'variable-name',
        why: `Describing an API that's defined elsewhere.`,
      }],
    });
    for (const analyzerDoc of analyzerDocs) {
      if (isEsModuleDocument(analyzerDoc)) {
        tsDoc.isEsModule = true;
      }
    }

    for (const analyzerDoc of analyzerDocs) {
      const generator = new TypeGenerator(
          tsDoc,
          analysis,
          analyzerDoc,
          rootDir,
          config.excludeIdentifiers || []);
      generator.handleDocument();
      warnings.push(...generator.warnings);
    }

    for (const ref of tsDoc.referencePaths) {
      const resolvedRef = path.resolve(rootDir, path.dirname(tsDoc.path), ref);
      if (removeReferencesResolved.has(resolvedRef)) {
        tsDoc.referencePaths.delete(ref);
      }
    }
    for (const ref of addReferences[tsDoc.path] || []) {
      tsDoc.referencePaths.add(path.relative(path.dirname(tsDoc.path), ref));
    }
    for (const node of tsDoc.traverse()) {
      if (node.kind === 'name') {
        const renamed = renameTypes.get(node.name);
        if (renamed !== undefined) {
          node.name = renamed;
        }
      }
    }
    addAutoImports(tsDoc, autoImportMap);
    tsDoc.simplify();

    // Include even documents with no members. They might be dependencies of
    // other files via the HTML import graph, and it's simpler to have empty
    // files than to try and prune the references (especially across packages).
    tsDocs.push(tsDoc);
  }

  const filteredWarnings = warnings.filter((warning) => {
    if (config.hideWarnings && warning.severity !== analyzer.Severity.ERROR) {
      return false;
    }
    const sourcePath =
        analyzerUrlToRelativePath(warning.sourceRange.file, rootDir);
    return sourcePath !== undefined &&
        !excludeFiles.some((pattern) => pattern.match(sourcePath));
  });
  const warningPrinter =
      new analyzer.WarningPrinter(process.stderr, {maxCodeLines: 1});
  await warningPrinter.printWarnings(filteredWarnings);
  if (filteredWarnings.some(
          (warning) => warning.severity === analyzer.Severity.ERROR)) {
    throw new Error('Encountered error generating types.');
  }

  if (config.googModules) {
    return tsDocs.map((d) => transformToGoogStyle(d, rootDir));
  }
  return tsDocs;
}

/**
 * Insert imports into the typings for any referenced identifiers listed in the
 * autoImport configuration, unless they are already imported.
 */
function addAutoImports(tsDoc: ts.Document, autoImport: Map<string, string>) {
  const alreadyImported = getImportedIdentifiers(tsDoc);

  for (const node of tsDoc.traverse()) {
    if (node.kind === 'name') {
      let importSpecifier = autoImport.get(node.name);
      if (importSpecifier === undefined) {
        continue;
      }
      if (alreadyImported.has(node.name)) {
        continue;
      }
      if (importSpecifier.startsWith('.')) {
        if (makeDeclarationsFilename(importSpecifier) === tsDoc.path) {
          // Don't import from yourself.
          continue;
        }
        importSpecifier =
            path.relative(path.dirname(tsDoc.path), importSpecifier);
        if (!importSpecifier.startsWith('.')) {
          importSpecifier = './' + importSpecifier;
        }
      }
      tsDoc.members.push(new ts.Import({
        identifiers: [{identifier: node.name}],
        fromModuleSpecifier: importSpecifier,
      }));
      alreadyImported.add(node.name);
    }
  }
}

function getPackageName(rootDir: string) {
  let packageInfo: {name?: string};
  try {
    packageInfo = JSON.parse(
        fsExtra.readFileSync(path.join(rootDir, 'package.json'), 'utf-8'));
  } catch {
    return undefined;
  }
  return packageInfo.name;
}

function googModuleForNameBasedImportSpecifier(spec: string) {
  const name =
      // remove trailing .d.ts and .js
      spec.replace(/(\.d\.ts|\.js)$/, '')
          // foo-bar.dom becomes fooBarDom
          .replace(/[-\.](\w)/g, (_, s) => s.toUpperCase())
          // remove leading @
          .replace(/^@/g, '')
          // slash separated paths becomes dot separated namespace
          .replace(/\//g, '.');

  // add goog: at the beginning
  return `goog:${name}`;
}

/* Note: this function modifies tsDoc. */
function transformToGoogStyle(tsDoc: ts.Document, rootDir: string) {
  const packageName = getPackageName(rootDir);
  if (!tsDoc.isEsModule || !packageName) {
    return tsDoc;
  }

  for (const child of tsDoc.traverse()) {
    if (child.kind === 'import' || child.kind === 'export') {
      if (!child.fromModuleSpecifier) {
        continue;
      }
      let spec = child.fromModuleSpecifier;
      if (spec.startsWith('.')) {
        spec = path.join(
            packageName,
            path.relative(
                    rootDir, path.join(rootDir, path.dirname(tsDoc.path), spec))
                .replace(/^\.\//, ''));
      }
      const elementName = spec.split('/')[1];
      let trailingComment: undefined|string = undefined;
      if (elementName && !/\./.test(elementName)) {
        trailingComment =
            ` // from //third_party/javascript/polymer/v2/${elementName}`;
      }
      const googSpecifier = googModuleForNameBasedImportSpecifier(spec);
      if (googSpecifier !== undefined) {
        child.fromModuleSpecifier = googSpecifier;
        child.trailingComment = trailingComment;
      }
    }
  }

  let googModuleName =
      googModuleForNameBasedImportSpecifier(path.join(packageName, tsDoc.path));
  if (googModuleName === undefined) {
    googModuleName = tsDoc.path;
  }
  return new ts.Document({
    path: tsDoc.path,
    header: tsDoc.header,
    referencePaths: tsDoc.referencePaths,
    tsLintDisables: tsDoc.tsLintDisables,
    isEsModule: false,
    members: [new ts.Namespace(
        {name: googModuleName, members: tsDoc.members, style: 'module'})]
  });
}

/**
 * Return all local identifiers imported by the given typings.
 */
function getImportedIdentifiers(tsDoc: ts.Document): Set<string> {
  const identifiers = new Set<string>();
  for (const member of tsDoc.members) {
    if (member.kind === 'import') {
      for (const {identifier, alias} of member.identifiers) {
        if (identifier !== ts.AllIdentifiers) {
          identifiers.add(alias || identifier);
        }
      }
    }
  }
  return identifiers;
}

/**
 * Analyzer always returns fully specified URLs with a protocol and an absolute
 * path (e.g. "file:/foo/bar"). Return just the file path, relative to our
 * project root.
 */
function analyzerUrlToRelativePath(
    analyzerUrl: string, rootDir: string): string|undefined {
  const parsed = Uri.parse(analyzerUrl);
  if (parsed.scheme !== 'file' || parsed.authority || !parsed.fsPath) {
    return undefined;
  }
  return path.relative(rootDir, parsed.fsPath);
}

/**
 * Create a TypeScript declarations filename for the given source document URL.
 * Simply replaces the file extension with `d.ts`.
 */
function makeDeclarationsFilename(sourceUrl: string): string {
  const parsed = path.parse(sourceUrl);
  return path.join(parsed.dir, parsed.name) + '.d.ts';
}

/**
 * Generate the header comment to show at the top of a declarations document.
 */
function makeHeader(sourceUrls: string[]): string {
  return `DO NOT EDIT

This file was automatically generated by
  https://github.com/Polymer/tools/tree/master/packages/gen-typescript-declarations

To modify these typings, edit the source file(s):
${sourceUrls.map((url) => '  ' + url).join('\n')}`;
}

class TypeGenerator {
  public warnings: analyzer.Warning[] = [];
  private excludeIdentifiers: Set<String>;

  /**
   * Identifiers in this set will always be considered resolvable, e.g.
   * for when determining what identifiers should be exported.
   */
  private forceResolvable = new Set<string>();

  constructor(
      private root: ts.Document, private analysis: analyzer.Analysis,
      private analyzerDoc: analyzer.Document, private rootDir: string,
      excludeIdentifiers: string[]) {
    this.excludeIdentifiers = new Set(excludeIdentifiers);
  }

  private warn(feature: analyzer.Feature, message: string) {
    this.warnings.push(new analyzer.Warning({
      message,
      sourceRange: feature.sourceRange!,
      severity: analyzer.Severity.WARNING,
      // We don't really need specific codes.
      code: 'GEN_TYPESCRIPT_DECLARATIONS_WARNING',
      parsedDocument: this.analyzerDoc.parsedDocument,
    }));
  }

  /**
   * Extend the given TypeScript declarations document with all of the relevant
   * items in the given Polymer Analyzer document.
   */
  handleDocument() {
    for (const feature of this.analyzerDoc.getFeatures()) {
      if ([...feature.identifiers].some(
              (id) => this.excludeIdentifiers.has(id))) {
        continue;
      }
      if (isPrivate(feature)) {
        continue;
      }
      if (feature.kinds.has('element')) {
        this.handleElement(feature as analyzer.Element);
      } else if (feature.kinds.has('behavior')) {
        this.handleBehavior(feature as analyzer.PolymerBehavior);
      } else if (feature.kinds.has('element-mixin')) {
        this.handleMixin(feature as analyzer.ElementMixin);
      } else if (feature.kinds.has('class')) {
        this.handleClass(feature as analyzer.Class);
      } else if (feature.kinds.has('function')) {
        this.handleFunction(feature as AnalyzerFunction);
      } else if (feature.kinds.has('namespace')) {
        this.handleNamespace(feature as analyzer.Namespace);
      } else if (feature.kinds.has('html-import')) {
        // Sometimes an Analyzer document includes an import feature that is
        // inbound (things that depend on me) instead of outbound (things I
        // depend on). For example, if an HTML file has a <script> tag for a JS
        // file, then the JS file's Analyzer document will include that <script>
        // tag as an import feature. We only care about outbound dependencies,
        // hence this check.
        if (feature.sourceRange &&
            feature.sourceRange.file === this.analyzerDoc.url) {
          this.handleHtmlImport(feature as analyzer.Import);
        }
      } else if (feature.kinds.has('js-import')) {
        this.handleJsImport(feature as analyzer.JavascriptImport);
      } else if (feature.kinds.has('export')) {
        this.handleJsExport(feature as analyzer.Export);
      }
    }
  }

  /**
   * Add the given Element to the given TypeScript declarations document.
   */
  private handleElement(feature: analyzer.Element) {
    // Whether this element has a constructor that is assigned and can be
    // called. If it does we'll emit a class, otherwise an interface.
    let constructable;

    let fullName;   // Fully qualified reference, e.g. `Polymer.DomModule`.
    let shortName;  // Just the last part of the name, e.g. `DomModule`.
    let parent;     // Where in the namespace tree does this live.

    if (feature.className) {
      constructable = true;
      let namespacePath;
      [namespacePath, shortName] = splitReference(feature.className);
      fullName = feature.className;
      parent = findOrCreateNamespace(this.root, namespacePath);

    } else if (feature.tagName) {
      // No `className` means this is an element defined by a call to the
      // Polymer function without a LHS assignment. We'll follow the convention
      // of the Closure Polymer Pass, and emit a global namespace interface
      // called `FooBarElement` (given a `tagName` of `foo-bar`). More context
      // here:
      //
      // https://github.com/google/closure-compiler/wiki/Polymer-Pass#element-type-names-for-1xhybrid-call-syntax
      // https://github.com/google/closure-compiler/blob/master/src/com/google/javascript/jscomp/PolymerClassDefinition.java#L128
      constructable = false;
      shortName = kebabToCamel(feature.tagName) + 'Element';
      fullName = shortName;
      parent = this.root;

    } else {
      this.warn(feature, `Could not find element name.`);
      return;
    }

    const legacyPolymerInterfaces = [];
    if (isPolymerElement(feature)) {
      legacyPolymerInterfaces.push(...feature.behaviorAssignments.map(
          (behavior) => behavior.identifier));

      if (feature.isLegacyFactoryCall) {
        if (this.root.isEsModule) {
          legacyPolymerInterfaces.push('LegacyElementMixin');
          if (!getImportedIdentifiers(this.root).has('LegacyElementMixin')) {
            this.root.members.push(new ts.Import({
              identifiers: [{identifier: 'LegacyElementMixin'}],
              fromModuleSpecifier:
                  '@polymer/polymer/lib/legacy/legacy-element-mixin.js',
            }));
          }

        } else {
          legacyPolymerInterfaces.push('Polymer.LegacyElementMixin');
        }

        legacyPolymerInterfaces.push('HTMLElement');
      }
    }

    if (constructable) {
      this.handleClass(feature);

      if (legacyPolymerInterfaces.length > 0) {
        // Augment the class interface.
        parent.members.push(new ts.Interface({
          name: shortName,
          extends: legacyPolymerInterfaces,
        }));
      }

    } else {
      parent.members.push(new ts.Interface({
        name: shortName,
        description: feature.description || feature.summary,
        properties: this.handleProperties(feature.properties.values()),
        // Don't worry about about static methods when we're not
        // constructable. Since there's no handle to the constructor, they
        // could never be called.
        methods: this.handleMethods(feature.methods.values()),
        extends: [
          ...feature.mixins.map((mixin) => mixin.identifier),
          ...legacyPolymerInterfaces,
        ],
      }));

      if (isPolymerElement(feature) && feature.isLegacyFactoryCall &&
          this.root.isEsModule) {
        this.root.members.push(
            new ts.Export({identifiers: [{identifier: shortName}]}));
      }
    }

    // The `HTMLElementTagNameMap` global interface maps custom element tag
    // names to their definitions, so that TypeScript knows that e.g.
    // `dom.createElement('my-foo')` returns a `MyFoo`. Augment the map with
    // this custom element.
    if (feature.tagName) {
      const elementMap = findOrCreateInterface(
          this.root.isEsModule ? findOrCreateGlobalNamespace(this.root) :
                                 this.root,
          'HTMLElementTagNameMap');
      elementMap.properties.push(new ts.Property({
        name: feature.tagName,
        type: new ts.NameType(fullName),
      }));
    }
  }

  /**
   * Add the given Polymer Behavior to the given TypeScript declarations
   * document.
   */
  private handleBehavior(feature: analyzer.PolymerBehavior) {
    if (!feature.className) {
      this.warn(feature, `Could not find a name for behavior.`);
      return;
    }

    const [namespacePath, className] = splitReference(feature.className);
    const ns = findOrCreateNamespace(this.root, namespacePath);

    // An interface with the properties and methods that this behavior adds to
    // an element. Note that behaviors are not classes, they are just data
    // objects which the Polymer library uses to augment element classes.
    ns.members.push(new ts.Interface({
      name: className,
      description: feature.description || feature.summary,
      extends: feature.behaviorAssignments.map((b) => b.identifier),
      properties: this.handleProperties(feature.properties.values()),
      methods: this.handleMethods(feature.methods.values()),
    }));

    // The value that contains the actual definition of the behavior for
    // Polymer. It's not important to know the shape of this object, so the
    // `object` type is good enough. The main use of this is to make statements
    // like `Polymer.mixinBehaviors([Polymer.SomeBehavior], ...)` compile.
    ns.members.push(new ts.ConstValue({
      name: className,
      type: new ts.NameType('object'),
    }));
  }

  /**
   * Add the given Mixin to the given TypeScript declarations document.
   */
  private handleMixin(feature: analyzer.ElementMixin) {
    const [namespacePath, mixinName] = splitReference(feature.name);
    const parentNamespace = findOrCreateNamespace(this.root, namespacePath);
    const transitiveMixins = [...this.transitiveMixins(feature)];
    const constructorName = mixinName + 'Constructor';

    // The mixin function. It takes a constructor, and returns an intersection
    // of 1) the given constructor, 2) the constructor for this mixin, 3) the
    // constructors for any other mixins that this mixin also applies.
    parentNamespace.members.push(new ts.Function({
      name: mixinName,
      description: feature.description,
      templateTypes: ['T extends new (...args: any[]) => {}'],
      params: [
        new ts.ParamType({name: 'base', type: new ts.NameType('T')}),
      ],
      returns: new ts.IntersectionType([
        new ts.NameType('T'),
        new ts.NameType(constructorName),
        ...transitiveMixins.map(
            (mixin) => new ts.NameType(mixin.name + 'Constructor'))
      ]),
    }));

    if (this.root.isEsModule) {
      // We need to import all of the synthetic constructor interfaces that our
      // own signature references. We can assume they're exported from the same
      // module that the mixin is defined in.
      for (const mixin of transitiveMixins) {
        if (mixin.sourceRange === undefined) {
          continue;
        }
        const rootRelative =
            analyzerUrlToRelativePath(mixin.sourceRange.file, this.rootDir);
        if (rootRelative === undefined) {
          continue;
        }
        const fileRelative =
            path.relative(path.dirname(this.root.path), rootRelative);
        const fromModuleSpecifier =
            fileRelative.startsWith('.') ? fileRelative : './' + fileRelative;
        const identifiers = [{identifier: mixin.name + 'Constructor'}];
        if (!getImportedIdentifiers(this.root).has(mixin.name)) {
          identifiers.push({identifier: mixin.name});
        }
        this.root.members.push(new ts.Import({
          identifiers,
          fromModuleSpecifier,
        }));
      }
    }

    // The interface for a constructor of this mixin. Returns the instance
    // interface (see below) when instantiated, and may also have methods of its
    // own (static methods from the mixin class).
    parentNamespace.members.push(new ts.Interface({
      name: constructorName,
      methods: [
        new ts.Method({
          name: 'new',
          params: [
            new ts.ParamType({
              name: 'args',
              type: new ts.ArrayType(ts.anyType),
              rest: true,
            }),
          ],
          returns: new ts.NameType(mixinName),
        }),
        ...this.handleMethods(feature.staticMethods.values()),
      ],
    }));

    if (this.root.isEsModule) {
      // If any other mixin applies us, it will need to import our synthetic
      // constructor interface.
      this.root.members.push(
          new ts.Export({identifiers: [{identifier: constructorName}]}));
    }

    // The interface for instances of this mixin. Has the same name as the
    // function.
    parentNamespace.members.push(
        new ts.Interface({
          name: mixinName,
          properties: this.handleProperties(feature.properties.values()),
          methods: this.handleMethods(feature.methods.values()),
          extends: transitiveMixins.map((mixin) => mixin.name),
        }),
    );
  }

  /**
   * Mixins can automatically apply other mixins, indicated by the @appliesMixin
   * annotation. However, since those mixins may themselves apply other mixins,
   * to know the full set of them we need to traverse down the tree.
   */
  private transitiveMixins(
      parentMixin: analyzer.ElementMixin,
      result?: Set<analyzer.ElementMixin>): Set<analyzer.ElementMixin> {
    if (result === undefined) {
      result = new Set();
    }
    for (const childRef of parentMixin.mixins) {
      const childMixinSet = this.analysis.getFeatures(
          {id: childRef.identifier, kind: 'element-mixin'});
      if (childMixinSet.size !== 1) {
        this.warn(
            parentMixin,
            `Found ${childMixinSet.size} features for mixin ` +
                `${childRef.identifier}, expected 1.`);
        continue;
      }
      const childMixin = childMixinSet.values().next().value;
      result.add(childMixin);
      this.transitiveMixins(childMixin, result);
    }
    return result;
  }

  /**
   * Add the given Class to the given TypeScript declarations document.
   */
  private handleClass(feature: analyzer.Class) {
    if (!feature.className) {
      this.warn(feature, `Could not find a name for class.`);
      return;
    }
    const [namespacePath, name] = splitReference(feature.className);
    const m = new ts.Class({name});
    m.description = feature.description;
    m.properties = this.handleProperties(feature.properties.values());
    m.methods = [
      ...this.handleMethods(feature.staticMethods.values(), {isStatic: true}),
      ...this.handleMethods(feature.methods.values())
    ];
    m.constructorMethod =
        this.handleConstructorMethod(feature.constructorMethod);
    if (feature.superClass !== undefined) {
      m.extends = feature.superClass.identifier;
    }
    m.mixins = feature.mixins.map((mixin) => mixin.identifier);
    findOrCreateNamespace(this.root, namespacePath).members.push(m);
  }

  /**
   * Add the given Function to the given TypeScript declarations document.
   */
  private handleFunction(feature: AnalyzerFunction) {
    const [namespacePath, name] = splitReference(feature.name);

    const f = new ts.Function({
      name,
      description: feature.description || feature.summary,
      templateTypes: feature.templateTypes,
      returns: closureTypeToTypeScript(
          feature.return && feature.return.type, feature.templateTypes),
      returnsDescription: feature.return && feature.return.desc
    });

    for (const param of feature.params || []) {
      // TODO Handle parameter default values. Requires support from Analyzer
      // which only handles this for class method parameters currently.
      f.params.push(closureParamToTypeScript(
          param.name, param.type, feature.templateTypes));
    }

    findOrCreateNamespace(this.root, namespacePath).members.push(f);
  }

  /**
   * Convert the given Analyzer properties to their TypeScript declaration
   * equivalent.
   */
  private handleProperties(analyzerProperties: Iterable<analyzer.Property>):
      ts.Property[] {
    const tsProperties = <ts.Property[]>[];
    for (const property of analyzerProperties) {
      if (property.inheritedFrom || property.privacy === 'private' ||
          this.excludeIdentifiers.has(property.name)) {
        continue;
      }
      const p = new ts.Property({
        name: property.name,
        // TODO If this is a Polymer property with no default value, then the
        // type should really be `<type>|undefined`.
        type: closureTypeToTypeScript(property.type),
        readOnly: property.readOnly,
      });
      p.description = property.description || '';
      if (property.jsdoc) {
        p.tags = property.jsdoc.tags || [];
      }
      tsProperties.push(p);
    }
    return tsProperties;
  }


  /**
   * Convert the given Analyzer methods to their TypeScript declaration
   * equivalent.
   */
  private handleMethods(analyzerMethods: Iterable<analyzer.Method>, opts?: {
    isStatic?: boolean
  }): ts.Method[] {
    const tsMethods = <ts.Method[]>[];
    for (const method of analyzerMethods) {
      if (method.inheritedFrom || method.privacy === 'private' ||
          this.excludeIdentifiers.has(method.name)) {
        continue;
      }

      tsMethods.push(this.handleMethod(method, opts));
    }
    return tsMethods;
  }

  /**
   * Convert the given Analyzer method to the equivalent TypeScript declaration
   */
  private handleMethod(method: analyzer.Method, opts?: {isStatic?: boolean}):
      ts.Method {
    const m = new ts.Method({
      name: method.name,
      returns: closureTypeToTypeScript(method.return && method.return.type),
      returnsDescription: method.return && method.return.desc,
      isStatic: opts && opts.isStatic,
      ignoreTypeCheck: this.documentationHasSuppressTypeCheck(method.jsdoc)
    });
    m.description = method.description || '';

    let requiredAhead = false;
    for (const param of reverseIter(method.params || [])) {
      const tsParam = closureParamToTypeScript(param.name, param.type);
      tsParam.description = param.description || '';

      if (param.defaultValue !== undefined) {
        // Parameters with default values generally behave like optional
        // parameters. However, unlike optional parameters, they may be
        // followed by a required parameter, in which case the default value is
        // set by explicitly passing undefined.
        if (!requiredAhead) {
          tsParam.optional = true;
        } else {
          tsParam.type = new ts.UnionType([tsParam.type, ts.undefinedType]);
        }
      } else if (!tsParam.optional) {
        requiredAhead = true;
      }

      // Analyzer might know this is a rest parameter even if there was no
      // JSDoc type annotation (or if it was wrong).
      tsParam.rest = tsParam.rest || !!param.rest;
      if (tsParam.rest && tsParam.type.kind !== 'array') {
        // Closure rest parameter types are written without the Array syntax,
        // but in TypeScript they must be explicitly arrays.
        tsParam.type = new ts.ArrayType(tsParam.type);
      }

      m.params.unshift(tsParam);
    }
    return m;
  }

  private documentationHasSuppressTypeCheck(annotation: jsdoc.Annotation|
                                            undefined): boolean {
    if (!annotation) {
      return false;
    }

    const annotationValue = annotation.tags.find((e) => e.title === 'suppress');
    return annotationValue && annotationValue.description === '{checkTypes}' ||
        false;
  }

  private handleConstructorMethod(method?: analyzer.Method): ts.Method
      |undefined {
    if (!method) {
      return;
    }
    const m = this.handleMethod(method);
    m.returns = undefined;
    return m;
  }

  /**
   * Add the given namespace to the given TypeScript declarations document.
   */
  private handleNamespace(feature: analyzer.Namespace) {
    const ns = findOrCreateNamespace(this.root, feature.name.split('.'));
    if (ns.kind === 'namespace') {
      ns.description = feature.description || feature.summary || '';
    }
  }

  /**
   * Add a JavaScript import to the TypeScript declarations.
   */
  private handleJsImport(feature: analyzer.JavascriptImport) {
    const node = feature.astNode.node;

    if (babel.isImportDeclaration(node)) {
      const identifiers: ts.ImportSpecifier[] = [];
      for (const specifier of node.specifiers) {
        if (babel.isImportSpecifier(specifier)) {
          // E.g. import {Foo, Bar as Baz} from './foo.js'
          if (this.isResolvable(specifier.imported.name, feature)) {
            identifiers.push({
              identifier: specifier.imported.name,
              alias: specifier.local.name,
            });
          }

        } else if (babel.isImportDefaultSpecifier(specifier)) {
          // E.g. import foo from './foo.js'
          if (this.isResolvable('default', feature)) {
            identifiers.push({
              identifier: 'default',
              alias: specifier.local.name,
            });
          }

        } else if (babel.isImportNamespaceSpecifier(specifier)) {
          // E.g. import * as foo from './foo.js'
          identifiers.push({
            identifier: ts.AllIdentifiers,
            alias: specifier.local.name,
          });
          this.forceResolvable.add(specifier.local.name);
        }
      }

      if (identifiers.length > 0) {
        this.root.members.push(new ts.Import({
          identifiers: identifiers,
          fromModuleSpecifier: node.source && node.source.value,
        }));
      }
    } else if (
        // Exports are handled as exports below. Analyzer also considers them
        // imports when they export from another module.
        !babel.isExportNamedDeclaration(node) &&
        !babel.isExportAllDeclaration(node)) {
      this.warn(feature, `Import with AST type ${node.type} not supported.`);
    }
  }


  /**
   * Add a JavaScript export to the TypeScript declarations.
   */
  private handleJsExport(feature: analyzer.Export) {
    const node = feature.astNode.node;

    if (babel.isExportAllDeclaration(node)) {
      // E.g. export * from './foo.js'
      this.root.members.push(new ts.Export({
        identifiers: ts.AllIdentifiers,
        fromModuleSpecifier: node.source && node.source.value,
      }));

    } else if (babel.isExportNamedDeclaration(node)) {
      const identifiers = [];

      if (node.declaration) {
        // E.g. export class Foo {}
        for (const identifier of feature.identifiers) {
          if (this.isResolvable(identifier, feature)) {
            identifiers.push({identifier});
          }
        }

      } else {
        // E.g. export {Foo, Bar as Baz}
        for (const specifier of node.specifiers) {
          if (this.isResolvable(specifier.exported.name, feature) ||
              this.isResolvable(specifier.local.name, feature)) {
            identifiers.push({
              identifier: specifier.local.name,
              alias: specifier.exported.name,
            });
          }
        }
      }

      if (identifiers.length > 0) {
        this.root.members.push(new ts.Export({
          identifiers,
          fromModuleSpecifier: node.source && node.source.value,
        }));
      }

    } else {
      this.warn(
          feature,
          `Export feature with AST node type ${node.type} not supported.`);
    }
  }

  /**
   * True if the given identifier can be resolved to a feature that will be
   * exported as a TypeScript type.
   */
  private isResolvable(
      identifier: string,
      fromFeature: analyzer.JavascriptImport|analyzer.Export) {
    if (this.forceResolvable.has(identifier)) {
      return true;
    }
    if (this.excludeIdentifiers.has(identifier)) {
      return false;
    }
    const resolved =
        resolveImportExportFeature(fromFeature, identifier, this.analyzerDoc);
    return resolved !== undefined && resolved.feature !== undefined &&
        !isPrivate(resolved.feature) && !isBehaviorImpl(resolved);
  }

  /**
   * Add an HTML import to a TypeScript declarations file. For a given HTML
   * import, we assume there is a corresponding declarations file that was
   * generated by this same process.
   */
  private handleHtmlImport(feature: analyzer.Import) {
    let sourcePath = analyzerUrlToRelativePath(feature.url, this.rootDir);
    if (sourcePath === undefined) {
      this.warn(
          feature,
          `Skipping HTML import without local file URL: ${feature.url}`);
      return;
    }
    // When we analyze a package's Git repo, our dependencies are installed to
    // "<repo>/bower_components". However, when this package is itself installed
    // as a dependency, our own dependencies will instead be siblings, one
    // directory up the tree.
    //
    // Analyzer (since 2.5.0) will set an import feature's URL to the resolved
    // dependency path as discovered on disk. An import for "../foo/foo.html"
    // will be resolved to "bower_components/foo/foo.html". Transform the URL
    // back to the style that will work when this package is installed as a
    // dependency.
    sourcePath =
        sourcePath.replace(/^(bower_components|node_modules)\//, '../');

    // Polymer is a special case where types are output to the "types/"
    // subdirectory instead of as sibling files, in order to avoid cluttering
    // the repo. It would be more pure to store this fact in the Polymer
    // gen-tsd.json config file and discover it when generating types for repos
    // that depend on it, but that's probably more complicated than we need,
    // assuming no other repos deviate from emitting their type declarations as
    // sibling files.
    sourcePath = sourcePath.replace(/^\.\.\/polymer\//, '../polymer/types/');

    this.root.referencePaths.add(path.relative(
        path.dirname(this.root.path), makeDeclarationsFilename(sourcePath)));
  }
}

/**
 * Iterate over an array backwards.
 */
function* reverseIter<T>(arr: T[]) {
  for (let i = arr.length - 1; i >= 0; i--) {
    yield arr[i];
  }
}

/**
 * Find a document's global namespace declaration, or create one if it doesn't
 * exist.
 */
function findOrCreateGlobalNamespace(doc: ts.Document): ts.GlobalNamespace {
  for (const member of doc.members) {
    if (member.kind === 'globalNamespace') {
      return member;
    }
  }
  const globalNamespace = new ts.GlobalNamespace();
  doc.members.push(globalNamespace);
  return globalNamespace;
}

/**
 * Traverse the given node to find the namespace AST node with the given path.
 * If it could not be found, add one and return it.
 */
function findOrCreateNamespace(
    root: ts.Document|ts.Namespace|ts.GlobalNamespace,
    path: string[]): ts.Document|ts.Namespace|ts.GlobalNamespace {
  if (!path.length) {
    return root;
  }
  let first: ts.Namespace|undefined;
  for (const member of root.members) {
    if (member.kind === 'namespace' && member.name === path[0]) {
      first = member;
      break;
    }
  }
  if (!first) {
    first = new ts.Namespace({name: path[0]});
    root.members.push(first);
  }
  return findOrCreateNamespace(first, path.slice(1));
}

/**
 * Traverse the given node to find the interface AST node with the given path.
 * If it could not be found, add one and return it.
 */
function findOrCreateInterface(
    root: ts.Document|ts.Namespace|ts.GlobalNamespace,
    reference: string): ts.Interface {
  const [namespacePath, name] = splitReference(reference);
  const namespace_ = findOrCreateNamespace(root, namespacePath);
  for (const member of namespace_.members) {
    if (member.kind === 'interface' && member.name === name) {
      return member;
    }
  }
  const i = new ts.Interface({name});
  namespace_.members.push(i);
  return i;
}

/**
 * Type guard that checks if a Polymer Analyzer feature is a PolymerElement.
 */
function isPolymerElement(feature: analyzer.Feature):
    feature is analyzer.PolymerElement {
  return feature.kinds.has('polymer-element');
}

/**
 * Return whether a reference looks like it is a FooBehaviorImpl style behavior
 * object, which we want to ignore.
 *
 * Polymer behavior libraries are often written like:
 *
 *   /** @polymerBehavior FooBehavior *\/
 *   export const FooBehaviorImpl = {};
 *
 *   /** @polymerBehavior *\/
 *   export const FooBehavior = [FooBehaviorImpl, OtherBehavior];
 *
 * In this case, Analyzer merges FooBehaviorImpl into FooBehavior and does not
 * emit a behavior feature for FooBehaviorImpl. However, there is still an
 * export feature for FooBehaviorImpl, so we exclude it here.
 */
function isBehaviorImpl(reference: analyzer.Reference<analyzer.Feature>) {
  return reference.feature !== undefined &&
      reference.feature.kinds.has('behavior') &&
      (reference.feature as analyzer.PolymerBehavior).name !==
      reference.identifier;
}

interface MaybePrivate {
  privacy?: 'public'|'private'|'protected';
}

/**
 * Return whether the given Analyzer feature has "private" visibility.
 */
function isPrivate(feature: analyzer.Feature&MaybePrivate): boolean {
  return feature.privacy === 'private';
}

/**
 * Convert kebab-case to CamelCase.
 */
function kebabToCamel(s: string): string {
  return s.replace(/(^|-)(.)/g, (_match, _p0, p1) => p1.toUpperCase());
}

/**
 * Split a reference into an array of namespace path parts, and a name part
 * (e.g. `"Foo.Bar.Baz"` => `[ ["Foo", "Bar"], "Baz" ]`).
 */
function splitReference(reference: string): [string[], string] {
  const parts = reference.split('.');
  const namespacePath = parts.slice(0, -1);
  const name = parts[parts.length - 1];
  return [namespacePath, name];
}
