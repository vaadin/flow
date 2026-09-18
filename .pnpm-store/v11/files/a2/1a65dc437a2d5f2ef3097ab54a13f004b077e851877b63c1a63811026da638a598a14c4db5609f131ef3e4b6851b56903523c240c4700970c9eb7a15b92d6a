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
import * as dom5 from 'dom5';
import * as parse5 from 'parse5';
import { Analyzer, Document } from 'polymer-analyzer';
import { AnalysisContext } from 'polymer-analyzer/lib/core/analysis-context';
import { RawSourceMap } from 'source-map';
export declare function getExistingSourcemap(analyzer: Analyzer, sourceUrl: string, sourceContent: string): Promise<RawSourceMap | null>;
/**
 * For an inline script AST node, locate an existing source map URL comment.
 * If found, load that source map. If no source map URL comment is found,
 * create an identity source map.
 *
 * In both cases, the generated mappings reflect the relative position of
 * a token within the script tag itself (rather than the document). This
 * is because the final position within the document is not yet known. These
 * relative positions will be updated later to reflect the absolute position
 * within the bundled document.
 */
export declare function addOrUpdateSourcemapComment(analyzer: AnalysisContext | Analyzer, sourceUrl: string, sourceContent: string, originalLineOffset: number, originalFirstLineCharOffset: number, generatedLineOffset: number, generatedFirtLineCharOffset: number): Promise<string>;
/**
 * Update mappings in source maps within inline script elements to reflect
 * their absolute position within a bundle. Assumes existing mappings
 * are relative to their position within the script tag itself.
 */
export declare function updateSourcemapLocations(document: Document, ast: parse5.ASTNode): dom5.Node;
