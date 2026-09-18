"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const dom5 = require("dom5/lib/index-next");
const htmlMinifier = require("html-minifier");
const parse5 = require("parse5");
const externalJs = require("./external-js");
const html_splitter_1 = require("./html-splitter");
const js_transform_1 = require("./js-transform");
const p = dom5.predicates;
const isJsScript = p.AND(p.hasTagName('script'), p.OR(p.NOT(p.hasAttr('type')), p.hasAttrValue('type', 'text/javascript'), p.hasAttrValue('type', 'application/javascript'), p.hasAttrValue('type', 'module')));
const isJsScriptOrHtmlImport = p.OR(isJsScript, p.AND(p.hasTagName('link'), p.hasSpaceSeparatedAttrValue('rel', 'import')));
/**
 * Transform some HTML according to the given options.
 */
function htmlTransform(html, options) {
    if (options.js && options.js.moduleResolution === 'node' &&
        !options.js.filePath) {
        throw new Error('Cannot perform node module resolution without filePath.');
    }
    const document = parse5.parse(html, {
        locationInfo: true,
    });
    removeFakeNodes(document);
    const allScripts = [...dom5.queryAll(document, isJsScript)];
    const shouldTransformEsModuleToAmd = options.js &&
        options.js.transformModulesToAmd &&
        // Assume that if this document has a nomodule script, the author is
        // already handling browsers that don't support modules, and we don't
        // need to transform them (even if the configuration was set).
        // TODO(aomarks) Check this for HtmlSplitter case too.
        !allScripts.some((node) => dom5.hasAttribute(node, 'nomodule'));
    let wctScript, firstModuleScript;
    for (const script of allScripts) {
        const isModule = dom5.getAttribute(script, 'type') === 'module';
        if (isModule) {
            if (firstModuleScript === undefined) {
                firstModuleScript = script;
            }
            if (shouldTransformEsModuleToAmd) {
                transformEsModuleToAmd(script, options.js);
                continue; // Bypass the standard jsTransform below.
            }
        }
        const isInline = !dom5.hasAttribute(script, 'src');
        if (isInline) {
            // Note that scripts split by HtmlSplitter are always external, so we
            // don't have to check for that case here.
            const newJs = js_transform_1.jsTransform(dom5.getTextContent(script), Object.assign({}, options.js, { transformModulesToAmd: false }));
            dom5.setTextContent(script, newJs);
        }
        else if (wctScript === undefined) {
            const src = dom5.getAttribute(script, 'src') || '';
            if (src.includes('web-component-tester/browser.js') ||
                src.includes('wct-browser-legacy/browser.js') ||
                src.includes('wct-mocha/wct-mocha.js')) {
                wctScript = script;
            }
        }
    }
    if (options.injectAmdLoader && shouldTransformEsModuleToAmd &&
        firstModuleScript !== undefined) {
        const fragment = parse5.parseFragment('<script></script>\n');
        dom5.setTextContent(fragment.childNodes[0], externalJs.getAmdLoader());
        const amdLoaderScript = fragment.childNodes[0];
        // Inject as late as possible (just before the first module is declared, if
        // there is one) because there may be some UMD dependencies that we want to
        // continue to load in global mode instead of AMD mode (which is detected by
        // the presence of the `require` global).
        // TODO(aomarks) If we don't define require, we can inject earlier.
        dom5.insertBefore(firstModuleScript.parentNode, firstModuleScript, fragment);
        if (wctScript !== undefined) {
            addWctTimingHack(wctScript, amdLoaderScript);
        }
    }
    const injectScript = (js) => {
        const fragment = parse5.parseFragment('<script></script>\n');
        dom5.setTextContent(fragment.childNodes[0], js);
        const firstJsScriptOrHtmlImport = dom5.query(document, isJsScriptOrHtmlImport);
        if (firstJsScriptOrHtmlImport) {
            dom5.insertBefore(firstJsScriptOrHtmlImport.parentNode, firstJsScriptOrHtmlImport, fragment);
        }
        else {
            const headOrDocument = dom5.query(document, dom5.predicates.hasTagName('head')) || document;
            dom5.append(headOrDocument, fragment);
        }
    };
    let babelHelpers;
    switch (options.injectBabelHelpers) {
        case undefined:
        case 'none':
            break;
        case 'full':
            babelHelpers = externalJs.getBabelHelpersFull();
            break;
        case 'amd':
            babelHelpers = externalJs.getBabelHelpersAmd();
            break;
        default:
            const never = options.injectBabelHelpers;
            throw new Error(`Unknown injectBabelHelpers value: ${never}`);
    }
    if (babelHelpers !== undefined) {
        injectScript(babelHelpers);
    }
    if (options.injectRegeneratorRuntime === true) {
        injectScript(externalJs.getRegeneratorRuntime());
    }
    html = parse5.serialize(document);
    if (options.minifyHtml) {
        html = htmlMinifier.minify(html, {
            collapseWhitespace: true,
            removeComments: true,
        });
    }
    return html;
}
exports.htmlTransform = htmlTransform;
function transformEsModuleToAmd(script, jsOptions) {
    // We're not a module anymore.
    dom5.removeAttribute(script, 'type');
    if (html_splitter_1.scriptWasSplitByHtmlSplitter(script)) {
        // Nothing else to do here. If we're using HtmlSplitter, the JsTransformer
        // is responsible for doing this transformation.
        return;
    }
    const isExternal = dom5.hasAttribute(script, 'src');
    if (isExternal) {
        const src = dom5.getAttribute(script, 'src');
        dom5.removeAttribute(script, 'src');
        dom5.setTextContent(script, `define(['${src}']);`);
    }
    else {
        // Transform inline scripts with the AMD Babel plugin transformer.
        const newJs = js_transform_1.jsTransform(dom5.getTextContent(script), Object.assign({}, jsOptions, { transformModulesToAmd: true }));
        dom5.setTextContent(script, newJs);
    }
}
function addWctTimingHack(wctScript, amdLoaderScript) {
    // This looks like a Web Component Tester script, and we have converted ES
    // modules to AMD. Converting a module to AMD means that `DOMContentLoaded`
    // will fire before the AMD loader resolves and executes the modules. Since
    // WCT listens for `DOMContentLoaded`, this means test suites in modules will
    // not have been registered by the time WCT starts running tests.
    //
    // To address this, we inject a block of JS that uses WCT's `waitFor` hook
    // to defer running tests until our AMD modules have loaded. If WCT finds a
    // `waitFor`, it passes it a callback that will run the tests, instead of
    // running tests immediately.
    //
    // Note we must do this as late as possible, before the WCT script, because
    // users may be setting their own `waitFor` that musn't clobber ours.
    // Likewise we must call theirs if we find it.
    dom5.insertBefore(wctScript.parentNode, wctScript, parse5.parseFragment(`
<script>
  // Injected by polymer-build to defer WCT until all AMD modules are loaded.
  (function() {
    window.WCT = window.WCT || {};
    var originalWaitFor = window.WCT.waitFor;
    window.WCT.waitFor = function(cb) {
      window._wctCallback = function() {
        if (originalWaitFor) {
          originalWaitFor(cb);
        } else {
          cb();
        }
      };
    };
  }());
</script>
`));
    // Monkey patch `define` to keep track of loaded AMD modules. Note this
    // assumes that all modules are registered before `DOMContentLoaded`, but
    // that's an assumption WCT normally makes anyway. Do this right after the AMD
    // loader is loaded, and hence before the first module is registered.
    dom5.insertAfter(amdLoaderScript.parentNode, amdLoaderScript, parse5.parseFragment(`
<script>
  // Injected by polymer-build to defer WCT until all AMD modules are loaded.
  (function() {
    var originalDefine = window.define;
    var moduleCount = 0;
    window.define = function(deps, factory) {
      moduleCount++;
      originalDefine(deps, function() {
        if (factory) {
          factory.apply(undefined, arguments);
        }
        moduleCount--;
        if (moduleCount === 0) {
          window._wctCallback();
        }
      });
    };
  })();
</script>
`));
}
/**
 * parse5 will inject <html>, <head>, and <body> tags if they aren't already
 * there. Undo this so that we make fewer unnecessary transformations.
 *
 * Note that the given document must have been parsed with `locationInfo: true`,
 * or else this function will always remove these tags.
 *
 * TODO(aomarks) Move to dom5.
 */
function removeFakeNodes(document) {
    const suspects = [];
    const html = (document.childNodes || []).find((child) => child.tagName === 'html');
    if (html !== undefined) {
        suspects.push(html);
        for (const child of html.childNodes || []) {
            if (child.tagName === 'head' || child.tagName === 'body') {
                suspects.push(child);
            }
        }
    }
    for (const suspect of suspects) {
        // No location means it wasn't in the original source.
        if (!suspect.__location) {
            dom5.removeNodeSaveChildren(suspect);
        }
    }
}
//# sourceMappingURL=html-transform.js.map