"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const analyzer = require("polymer-analyzer");
/**
 * Return whether an Analyzer document is a JavaScript document which was parsed
 * as a module.
 */
function isEsModuleDocument(doc) {
    return doc.type === 'js' &&
        doc.parsedDocument
            .parsedAsSourceType === 'module';
}
exports.isEsModuleDocument = isEsModuleDocument;
/**
 * Resolve an identifier being imported or exported to the feature it refers to.
 */
function resolveImportExportFeature(feature, identifier, doc) {
    for (const kind of resolveKinds) {
        const reference = new analyzer.ScannedReference(kind, identifier, feature.sourceRange, feature.astNode, feature.astNodePath);
        const resolved = reference.resolve(doc);
        if (resolved.feature !== undefined) {
            return resolved;
        }
    }
    return undefined;
}
exports.resolveImportExportFeature = resolveImportExportFeature;
const resolveKinds = [
    'element',
    'behavior',
    'element-mixin',
    'class',
    'function',
    'namespace',
];
//# sourceMappingURL=es-modules.js.map