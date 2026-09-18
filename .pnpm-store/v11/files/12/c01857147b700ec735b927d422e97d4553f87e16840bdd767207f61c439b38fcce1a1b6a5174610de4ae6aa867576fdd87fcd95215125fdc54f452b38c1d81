/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

import * as analyzer from 'polymer-analyzer';

/**
 * Return whether an Analyzer document is a JavaScript document which was parsed
 * as a module.
 */
export function isEsModuleDocument(doc: analyzer.Document):
    doc is analyzer.Document<analyzer.ParsedJavaScriptDocument> {
  return doc.type === 'js' &&
      (doc.parsedDocument as analyzer.ParsedJavaScriptDocument)
          .parsedAsSourceType === 'module';
}

/**
 * Resolve an identifier being imported or exported to the feature it refers to.
 */
export function resolveImportExportFeature(
    feature: analyzer.JavascriptImport|analyzer.Export,
    identifier: string,
    doc: analyzer.Document): analyzer.Reference<ResolvedTypes>|undefined {
  for (const kind of resolveKinds) {
    const reference = new analyzer.ScannedReference(
        kind,
        identifier,
        feature.sourceRange,
        feature.astNode,
        feature.astNodePath);
    const resolved = reference.resolve(doc);
    if (resolved.feature !== undefined) {
      return resolved as analyzer.Reference<ResolvedTypes>;
    }
  }
  return undefined;
}

const resolveKinds: Array<keyof analyzer.FeatureKindMap> = [
  'element',
  'behavior',
  'element-mixin',
  'class',
  'function',
  'namespace',
];

export type ResolvedTypes = analyzer.Element|analyzer.PolymerBehavior|
                            analyzer.ElementMixin|analyzer.Class|
                            analyzer.Function|analyzer.Namespace;
