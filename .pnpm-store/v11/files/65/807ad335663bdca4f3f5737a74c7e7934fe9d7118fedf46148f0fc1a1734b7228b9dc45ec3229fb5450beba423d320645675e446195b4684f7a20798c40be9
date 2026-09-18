"use strict";
var __asyncValues = (this && this.__asyncValues) || function (o) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var m = o[Symbol.asyncIterator], i;
    return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i);
    function verb(n) { i[n] = o[n] && function (v) { return new Promise(function (resolve, reject) { v = o[n](v), settle(resolve, reject, v.done, v.value); }); }; }
    function settle(resolve, reject, d, v) { Promise.resolve(v).then(function(v) { resolve({ value: v, done: d }); }, reject); }
};
var __await = (this && this.__await) || function (v) { return this instanceof __await ? (this.v = v, this) : new __await(v); }
var __asyncGenerator = (this && this.__asyncGenerator) || function (thisArg, _arguments, generator) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var g = generator.apply(thisArg, _arguments || []), i, q = [];
    return i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i;
    function verb(n) { if (g[n]) i[n] = function (v) { return new Promise(function (a, b) { q.push([n, v, a, b]) > 1 || resume(n, v); }); }; }
    function resume(n, v) { try { step(g[n](v)); } catch (e) { settle(q[0][3], e); } }
    function step(r) { r.value instanceof __await ? Promise.resolve(r.value.v).then(fulfill, reject) : settle(q[0][2], r); }
    function fulfill(value) { resume("next", value); }
    function reject(value) { resume("throw", value); }
    function settle(f, v) { if (f(v), q.shift(), q.length) resume(q[0][0], q[0][1]); }
};
Object.defineProperty(exports, "__esModule", { value: true });
const dom5 = require("dom5/lib/index-next");
const index_next_1 = require("dom5/lib/index-next");
const parse5 = require("parse5");
const url = require("url");
const streams_1 = require("./streams");
const attrValueMatches = (attrName, regex) => {
    return (node) => {
        const attrValue = dom5.getAttribute(node, attrName);
        return attrValue != null && regex.test(attrValue);
    };
};
const webcomponentsLoaderRegex = /\bwebcomponents\-(loader|lite|bundle)\.js\b/;
const webcomponentsLoaderMatcher = index_next_1.predicates.AND(index_next_1.predicates.hasTagName('script'), attrValueMatches('src', webcomponentsLoaderRegex));
/**
 * Wraps `addCustomElementsEs5Adapter()` in a `stream.Transform`.
 */
class CustomElementsEs5AdapterInjector extends streams_1.AsyncTransformStream {
    constructor() {
        super({ objectMode: true });
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    if (file.contents === null || file.extname !== '.html') {
                        yield yield __await(file);
                        continue;
                    }
                    const contents = yield __await(streams_1.getFileContents(file));
                    const updatedContents = addCustomElementsEs5Adapter(contents);
                    if (contents === updatedContents) {
                        yield yield __await(file);
                    }
                    else {
                        const updatedFile = file.clone();
                        updatedFile.contents = Buffer.from(updatedContents, 'utf-8');
                        yield yield __await(updatedFile);
                    }
                }
            }
            catch (e_1_1) { e_1 = { error: e_1_1 }; }
            finally {
                try {
                    if (files_1_1 && !files_1_1.done && (_a = files_1.return)) yield __await(_a.call(files_1));
                }
                finally { if (e_1) throw e_1.error; }
            }
        });
    }
}
exports.CustomElementsEs5AdapterInjector = CustomElementsEs5AdapterInjector;
/**
 * Please avoid using this function because the API is likely to change. Prefer
 * the interface provided by `PolymerProject.addCustomElementsEs5Adapter`.
 *
 * When compiling ES6 classes down to ES5 we need to include a special shim so
 * that compiled custom elements will still work on browsers that support native
 * custom elements.
 *
 * TODO(fks) 03-28-2017: Add tests.
 */
function addCustomElementsEs5Adapter(html) {
    // Only modify this file if we find a web components polyfill. This is a
    // heuristic to identify the entry point HTML file. Ultimately we should
    // explicitly transform only the entry point by having the project config.
    if (!webcomponentsLoaderRegex.test(html)) {
        return html;
    }
    const parsed = parse5.parse(html, { locationInfo: true });
    const script = dom5.query(parsed, webcomponentsLoaderMatcher);
    if (!script) {
        return html;
    }
    // Collect important dom references & create fragments for injection.
    const loaderScriptUrl = dom5.getAttribute(script, 'src');
    const adapterScriptUrl = url.resolve(loaderScriptUrl, 'custom-elements-es5-adapter.js');
    const es5AdapterFragment = parse5.parseFragment(`
    <script>if (!window.customElements) { document.write('<!--'); }</script>
    <script type="text/javascript" src="${adapterScriptUrl}"></script>
    <!--! do not remove -->
`);
    dom5.insertBefore(script.parentNode, script, es5AdapterFragment);
    return parse5.serialize(parsed);
}
exports.addCustomElementsEs5Adapter = addCustomElementsEs5Adapter;
//# sourceMappingURL=custom-elements-es5-adapter.js.map