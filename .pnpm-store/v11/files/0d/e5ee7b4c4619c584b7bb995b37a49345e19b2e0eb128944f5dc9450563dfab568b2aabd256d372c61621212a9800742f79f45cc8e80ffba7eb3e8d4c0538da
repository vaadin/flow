/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
import { ASTNode } from 'parse5';
import { Document, ParsedHtmlDocument, ResolvedUrl } from 'polymer-analyzer';
import { AssignedBundle, BundleManifest } from './bundle-manifest';
import { Bundler } from './bundler';
import { BundledHtmlDocument } from './document-collection';
/**
 * Produces an HTML BundledDocument.
 */
export declare function bundle(bundler: Bundler, manifest: BundleManifest, url: ResolvedUrl): Promise<BundledHtmlDocument>;
/**
 * A single-use instance of this class produces a single HTML BundledDocument.
 * Use the bundle directly is deprecated; it is exported only to support unit
 * tests of its methods in html-bundler_test.ts for now.  Please use the
 * exported bundle function above.
 */
export declare class HtmlBundler {
    bundler: Bundler;
    assignedBundle: AssignedBundle;
    manifest: BundleManifest;
    protected document: Document<ParsedHtmlDocument>;
    constructor(bundler: Bundler, assignedBundle: AssignedBundle, manifest: BundleManifest);
    bundle(): Promise<BundledHtmlDocument>;
    /**
     * Walk through inline scripts of an import document.
     * For each script create identity source maps unless one already exists.
     *
     * The generated script mapping detail is the relative location within
     * the script tag. Later this will be updated to account for the
     * line offset within the final bundle.
     */
    private _addOrUpdateSourcemapsForInlineScripts;
    /**
     * Set the hidden div at the appropriate location within the document.  The
     * goal is to place the hidden div at the same place as the first html
     * import.  However, the div can't be placed in the `<head>` of the document
     * so if first import is found in the head, we prepend the div to the body.
     * If there is no body, we'll just attach the hidden div to the document at
     * the end.
     */
    private _attachHiddenDiv;
    /**
     * Creates a hidden container <div> to which inlined content will be
     * appended.
     */
    private _createHiddenDiv;
    /**
     * Append a `<link rel="import" ...>` node to `node` with a value of `url`
     * for the "href" attribute.
     */
    private _createHtmlImport;
    /**
     * Given a document, search for the hidden div, if it isn't found, then
     * create it.  After creating it, attach it to the desired location.  Then
     * return it.
     */
    private _findOrCreateHiddenDiv;
    /**
     * Add HTML Import elements for each file in the bundle.  Efforts are made
     * to ensure that imports are injected prior to any eager imports of other
     * bundles which are known to depend on them, to preserve expectations of
     * evaluation order.
     */
    private _injectHtmlImportsForBundle;
    /**
     * Inline the contents of the html document returned by the link tag's href
     * at the location of the link tag and then remove the link tag.  If the
     * link is a `lazy-import` link, content will not be inlined.
     */
    private _inlineHtmlImport;
    /**
     * Replace html import links in the document with the contents of the
     * imported file, but only once per URL.
     */
    private _inlineHtmlImports;
    /**
     * Update the `src` attribute of external `type=module` script tags to point
     * at new bundle locations.
     */
    _updateExternalModuleScriptTags(ast: ASTNode): Promise<void>;
    /**
     * Inlines the contents of external module scripts and rolls-up imported
     * modules into inline scripts.
     */
    private _rollupInlineModuleScripts;
    /**
     * Replace all external module script tags:
     * `<script type="module" src="..."></script>`
     * with inline script tags containing import:
     * `<script type="module">import '...';</script>`
     * And these will be subsequently rolled up by call to
     * `this._rollupInlineModuleScripts()`.
     */
    private _rewriteExternalModuleScriptTagsAsImports;
    /**
     * Inlines the contents of the document returned by the script tag's src URL
     * into the script tag content and removes the src attribute.
     */
    private _inlineNonModuleScript;
    /**
     * Replace all external javascript tags (`<script src="...">`)
     * with `<script>` tags containing the file contents inlined.
     */
    private _inlineNonModuleScripts;
    /**
     * Inlines the contents of the stylesheet returned by the link tag's href
     * URL into a style tag and removes the link tag.
     */
    private _inlineStylesheet;
    /**
     * Replace all polymer stylesheet imports (`<link rel="import" type="css">`)
     * with `<style>` tags containing the file contents, with internal URLs
     * relatively transposed as necessary.
     */
    private _inlineStylesheetImports;
    /**
     * Replace all external stylesheet references, in `<link rel="stylesheet">`
     * tags with `<style>` tags containing file contents, with internal URLs
     * relatively transposed as necessary.
     */
    private _inlineStylesheetLinks;
    /**
     * Old Polymer supported `<style>` tag in `<dom-module>` but outside of
     * `<template>`.  This is also where the deprecated Polymer CSS import tag
     * `<link rel="import" type="css">` would generate inline `<style>`.
     * Migrates these `<style>` tags into available `<template>` of the
     * `<dom-module>`.  Will create a `<template>` container if not present.
     *
     * TODO(usergenic): Why is this in bundler... shouldn't this be some kind of
     * polyup or pre-bundle operation?
     */
    private _moveDomModuleStyleIntoTemplate;
    /**
     * When an HTML Import is encountered in the head of the document, it needs
     * to be moved into the hidden div and any subsequent order-dependent
     * imperatives (imports, styles, scripts) must also be move into the
     * hidden div.
     */
    private _moveOrderedImperativesFromHeadIntoHiddenDiv;
    /**
     * Move any remaining htmlImports that are not inside the hidden div
     * already, into the hidden div.
     */
    private _moveUnhiddenHtmlImportsIntoHiddenDiv;
    /**
     * Generate a fresh document to bundle contents into.  If we're building
     * a bundle which is based on an existing file, we should load that file and
     * prepare it as the bundle document, otherwise we'll create a clean/empty
     * HTML document.
     */
    private _prepareBundleDocument;
    /**
     * Fetch a new copy of an analyzed document serializing an AST and analyzing
     * it.
     */
    private _reanalyze;
    /**
     * Removes all empty hidden container divs from the AST.
     */
    private _removeEmptyHiddenDivs;
    /**
     * Walk through an import document, and rewrite all URLs so they are
     * correctly relative to the main document URL as they've been
     * imported from the import URL.
     */
    private _rewriteAstBaseUrl;
    /**
     * Given an import document with a base tag, transform all of its URLs and
     * set link and form target attributes and remove the base tag.
     */
    private _rewriteAstToEmulateBaseTag;
    /**
     * Given a string of CSS, return a version where all occurrences of URLs,
     * have been rewritten based on the relationship of the old base URL to the
     * new base URL.
     */
    private _rewriteCssTextBaseUrl;
    /**
     * Find all element attributes which express URLs and rewrite them so they
     * are based on the relationship of the old base URL to the new base URL.
     */
    private _rewriteElementAttrsBaseUrl;
    private _rewriteHrefBaseUrl;
    /**
     * Find all URLs in imported style nodes and rewrite them so they are based
     * on the relationship of the old base URL to the new base URL.
     */
    private _rewriteStyleTagsBaseUrl;
    /**
     * Set the assetpath attribute of all imported dom-modules which don't yet
     * have them if the base URLs are different.
     */
    private _setDomModuleAssetpaths;
}
