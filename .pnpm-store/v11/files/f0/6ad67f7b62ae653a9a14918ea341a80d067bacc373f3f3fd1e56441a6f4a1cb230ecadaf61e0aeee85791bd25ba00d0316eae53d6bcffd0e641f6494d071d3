"use strict";
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
function __export(m) {
    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
}
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * This directory exposes our underlying models. The naming scheme is based on
 * the stage of the processing pipeline that an object is produced by.
 *
 * The pipeline looks like:
 *   load: reads in bytes from filesystem/network
 *   parse: convert bytes to abstract syntax trees
 *   scan: extract entirely local features from a single ast
 *   resolve: integrate cross-file information to produce the final result
 *
 * Importantly, we can cache everything before `resolve` at the file level,
 * making incremental analysis efficient.
 *
 * Because the output of the resolve phase is the API that will get the most
 * use, its results have clear, unprefixed names. So a resolved document is just
 * a Document, a resolved element is an Element.
 *
 * Earlier stages have the longer names, like ParsedDocument and ScannedElement.
 */
// TODO(rictic): export document and package query options...
var analysis_1 = require("./analysis");
exports.Analysis = analysis_1.Analysis;
__export(require("./class"));
var document_1 = require("./document");
exports.Document = document_1.Document;
exports.ScannedDocument = document_1.ScannedDocument;
var document_backreference_1 = require("./document-backreference");
exports.DocumentBackreference = document_backreference_1.DocumentBackreference;
__export(require("./element"));
__export(require("./element-base"));
__export(require("./element-mixin"));
var element_reference_1 = require("./element-reference");
exports.ElementReference = element_reference_1.ElementReference;
exports.ScannedElementReference = element_reference_1.ScannedElementReference;
__export(require("./feature"));
__export(require("./import"));
__export(require("./inline-document"));
__export(require("./map"));
__export(require("./reference"));
__export(require("./resolvable"));
__export(require("./source-range"));
__export(require("./url"));
__export(require("./warning"));
//# sourceMappingURL=model.js.map