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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
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
const assert = require("assert");
const dom5 = require("dom5/lib/index-next");
const parse5 = require("parse5");
const osPath = require("path");
const File = require("vinyl");
const streams_1 = require("./streams");
const pred = dom5.predicates;
const extensionsForType = {
    'text/ecmascript-6': 'js',
    'application/javascript': 'js',
    'text/javascript': 'js',
    'application/x-typescript': 'ts',
    'text/x-typescript': 'ts',
    'module': 'js',
};
/**
 * HTMLSplitter represents the shared state of files as they are passed through
 * a splitting stream and then a rejoining stream. Creating a new instance of
 * HTMLSplitter and adding its streams to the build pipeline is the
 * supported user interface for splitting out and rejoining inlined CSS & JS in
 * the build process.
 */
class HtmlSplitter {
    constructor() {
        this._splitFiles = new Map();
        this._parts = new Map();
    }
    /**
     * Returns a new `Transform` stream that splits inline script and styles into
     * new, separate files that are passed out of the stream.
     */
    split() {
        return new HtmlSplitTransform(this);
    }
    /**
     * Returns a new `Transform` stream that rejoins inline scripts and styles
     * that were originally split from this `HTMLSplitter`'s `split()` back into
     * their parent HTML files.
     */
    rejoin() {
        return new HtmlRejoinTransform(this);
    }
    isSplitFile(parentPath) {
        return this._splitFiles.has(parentPath);
    }
    getSplitFile(parentPath) {
        // TODO(justinfagnani): rewrite so that processing a parent file twice
        // throws to protect against bad configurations of multiple streams that
        // contain the same file multiple times.
        let splitFile = this._splitFiles.get(parentPath);
        if (!splitFile) {
            splitFile = new SplitFile(parentPath);
            this._splitFiles.set(parentPath, splitFile);
        }
        return splitFile;
    }
    addSplitPath(parentPath, childPath) {
        const splitFile = this.getSplitFile(parentPath);
        splitFile.addPartPath(childPath);
        this._parts.set(childPath, splitFile);
    }
    getParentFile(childPath) {
        return this._parts.get(childPath);
    }
}
exports.HtmlSplitter = HtmlSplitter;
const htmlSplitterAttribute = 'html-splitter';
/**
 * Returns whether the given script tag was an inline script that was split out
 * into a fake file by HtmlSplitter.
 */
function scriptWasSplitByHtmlSplitter(script) {
    return dom5.hasAttribute(script, htmlSplitterAttribute);
}
exports.scriptWasSplitByHtmlSplitter = scriptWasSplitByHtmlSplitter;
/**
 * Return whether the given Vinyl file was created by the HtmlSplitter from an
 * HTML document script tag.
 */
function isHtmlSplitterFile(file) {
    return file.fromHtmlSplitter === true;
}
exports.isHtmlSplitterFile = isHtmlSplitterFile;
/**
 * Represents a file that is split into multiple files.
 */
class SplitFile {
    constructor(path) {
        this.parts = new Map();
        this.outstandingPartCount = 0;
        this.vinylFile = null;
        this.path = path;
    }
    addPartPath(path) {
        this.parts.set(path, null);
        this.outstandingPartCount++;
    }
    setPartContent(path, content) {
        assert(this.parts.get(path) !== undefined, `Trying to save unexpected file part "${path}".`);
        assert(this.parts.get(path) === null, `Trying to save already-saved file part "${path}".`);
        assert(this.outstandingPartCount > 0, `Trying to save valid file part "${path}", ` +
            `but somehow no file parts are outstanding.`);
        this.parts.set(path, content);
        this.outstandingPartCount--;
    }
    get isComplete() {
        return this.outstandingPartCount === 0 && this.vinylFile != null;
    }
}
exports.SplitFile = SplitFile;
/**
 * Splits HTML files, extracting scripts and styles into separate File objects.
 */
class HtmlSplitTransform extends streams_1.AsyncTransformStream {
    constructor(splitter) {
        super({ objectMode: true });
        this._state = splitter;
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    const filePath = osPath.normalize(file.path);
                    if (!(file.contents && filePath.endsWith('.html'))) {
                        yield yield __await(file);
                        continue;
                    }
                    const contents = yield __await(streams_1.getFileContents(file));
                    const doc = parse5.parse(contents, { locationInfo: true });
                    dom5.removeFakeRootElements(doc);
                    const scriptTags = [...dom5.queryAll(doc, pred.hasTagName('script'))];
                    for (let i = 0; i < scriptTags.length; i++) {
                        const scriptTag = scriptTags[i];
                        const source = dom5.getTextContent(scriptTag);
                        const typeAttribute = dom5.getAttribute(scriptTag, 'type') || 'application/javascript';
                        const extension = extensionsForType[typeAttribute];
                        // If we don't recognize the script type attribute, don't split
                        // out.
                        if (!extension) {
                            continue;
                        }
                        const isInline = !dom5.hasAttribute(scriptTag, 'src');
                        if (isInline) {
                            const childFilename = `${osPath.basename(filePath)}_script_${i}.${extension}`;
                            const childPath = osPath.join(osPath.dirname(filePath), childFilename);
                            scriptTag.childNodes = [];
                            dom5.setAttribute(scriptTag, 'src', childFilename);
                            dom5.setAttribute(scriptTag, htmlSplitterAttribute, '');
                            const scriptFile = new File({
                                cwd: file.cwd,
                                base: file.base,
                                path: childPath,
                                contents: Buffer.from(source),
                            });
                            scriptFile.fromHtmlSplitter = true;
                            scriptFile.isModule = typeAttribute === 'module';
                            this._state.addSplitPath(filePath, childPath);
                            this.push(scriptFile);
                        }
                    }
                    const splitContents = parse5.serialize(doc);
                    const newFile = new File({
                        cwd: file.cwd,
                        base: file.base,
                        path: filePath,
                        contents: Buffer.from(splitContents),
                    });
                    yield yield __await(newFile);
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
/**
 * Joins HTML files originally split by `Splitter`, based on the relationships
 * stored in its HTMLSplitter state.
 */
class HtmlRejoinTransform extends streams_1.AsyncTransformStream {
    constructor(splitter) {
        super({ objectMode: true });
        this._state = splitter;
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_2() {
            var e_2, _a;
            try {
                for (var files_2 = __asyncValues(files), files_2_1; files_2_1 = yield __await(files_2.next()), !files_2_1.done;) {
                    const file = files_2_1.value;
                    const filePath = osPath.normalize(file.path);
                    if (this._state.isSplitFile(filePath)) {
                        // this is a parent file
                        const splitFile = this._state.getSplitFile(filePath);
                        splitFile.vinylFile = file;
                        if (splitFile.isComplete) {
                            yield yield __await(yield __await(this._rejoin(splitFile)));
                        }
                        else {
                            splitFile.vinylFile = file;
                        }
                    }
                    else {
                        const parentFile = this._state.getParentFile(filePath);
                        if (parentFile) {
                            // this is a child file
                            parentFile.setPartContent(filePath, file.contents.toString());
                            if (parentFile.isComplete) {
                                yield yield __await(yield __await(this._rejoin(parentFile)));
                            }
                        }
                        else {
                            yield yield __await(file);
                        }
                    }
                }
            }
            catch (e_2_1) { e_2 = { error: e_2_1 }; }
            finally {
                try {
                    if (files_2_1 && !files_2_1.done && (_a = files_2.return)) yield __await(_a.call(files_2));
                }
                finally { if (e_2) throw e_2.error; }
            }
        });
    }
    _rejoin(splitFile) {
        return __awaiter(this, void 0, void 0, function* () {
            const file = splitFile.vinylFile;
            if (file == null) {
                throw new Error(`Internal error: no vinylFile found for splitfile: ${splitFile.path}`);
            }
            const filePath = osPath.normalize(file.path);
            const contents = yield streams_1.getFileContents(file);
            const doc = parse5.parse(contents, { locationInfo: true });
            dom5.removeFakeRootElements(doc);
            const scriptTags = dom5.queryAll(doc, HtmlRejoinTransform.isExternalScript);
            for (const scriptTag of scriptTags) {
                const srcAttribute = dom5.getAttribute(scriptTag, 'src');
                const scriptPath = osPath.join(osPath.dirname(splitFile.path), srcAttribute);
                const scriptSource = splitFile.parts.get(scriptPath);
                if (scriptSource != null) {
                    dom5.setTextContent(scriptTag, scriptSource);
                    dom5.removeAttribute(scriptTag, 'src');
                    dom5.removeAttribute(scriptTag, htmlSplitterAttribute);
                }
            }
            const joinedContents = parse5.serialize(doc);
            return new File({
                cwd: file.cwd,
                base: file.base,
                path: filePath,
                contents: Buffer.from(joinedContents),
            });
        });
    }
}
HtmlRejoinTransform.isExternalScript = pred.AND(pred.hasTagName('script'), pred.hasAttr('src'));
//# sourceMappingURL=html-splitter.js.map