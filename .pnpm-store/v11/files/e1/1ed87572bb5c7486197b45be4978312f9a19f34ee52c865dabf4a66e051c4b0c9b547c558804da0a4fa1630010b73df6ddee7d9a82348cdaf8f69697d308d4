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
Object.defineProperty(exports, "__esModule", { value: true });
const cssSlam = require("css-slam");
const gulpif = require("gulp-if");
const logging = require("plylog");
const stream_1 = require("stream");
const matcher = require("matcher");
const js_transform_1 = require("./js-transform");
const html_transform_1 = require("./html-transform");
const html_splitter_1 = require("./html-splitter");
const logger = logging.getLogger('cli.build.optimize-streams');
/**
 * GenericOptimizeTransform is a generic optimization stream. It can be extended
 * to create a new kind of specific file-type optimizer, or it can be used
 * directly to create an ad-hoc optimization stream for different libraries.
 * If the transform library throws an exception when run, the file will pass
 * through unaffected.
 */
class GenericOptimizeTransform extends stream_1.Transform {
    constructor(optimizerName, optimizer) {
        super({ objectMode: true });
        this.optimizer = optimizer;
        this.optimizerName = optimizerName;
    }
    _transform(file, _encoding, callback) {
        // TODO(fks) 03-07-2017: This is a quick fix to make sure that
        // "webcomponentsjs" files aren't compiled down to ES5, because they contain
        // an important ES6 shim to make custom elements possible. Remove/refactor
        // when we have a better plan for excluding some files from optimization.
        if (!file.path || file.path.indexOf('webcomponentsjs/') >= 0 ||
            file.path.indexOf('webcomponentsjs\\') >= 0) {
            callback(undefined, file);
            return;
        }
        if (file.contents) {
            try {
                let contents = file.contents.toString();
                contents = this.optimizer(contents, file);
                file.contents = Buffer.from(contents);
            }
            catch (error) {
                logger.warn(`${this.optimizerName}: Unable to optimize ${file.path}`, { err: error.message || error });
            }
        }
        callback(undefined, file);
    }
}
exports.GenericOptimizeTransform = GenericOptimizeTransform;
function getCompileTarget(file, options) {
    let target;
    const compileOptions = options.compile;
    if (notExcluded(options.compile)(file)) {
        if (typeof compileOptions === 'object') {
            target =
                (compileOptions.target === undefined) ? true : compileOptions.target;
        }
        else {
            target = compileOptions;
        }
        if (target === undefined) {
            target = false;
        }
    }
    else {
        target = false;
    }
    return target;
}
/**
 * Transform JavaScript.
 */
class JsTransform extends GenericOptimizeTransform {
    constructor(options) {
        const jsOptions = options.js || {};
        const shouldMinifyFile = jsOptions.minify ? notExcluded(jsOptions.minify) : () => false;
        const transformer = (content, file) => {
            let transformModulesToAmd = false;
            if (jsOptions.transformModulesToAmd) {
                if (html_splitter_1.isHtmlSplitterFile(file)) {
                    // This is a type=module script in an HTML file. Definitely AMD
                    // transform.
                    transformModulesToAmd = file.isModule === true;
                }
                else {
                    // This is an external script file. Only AMD transform it if it looks
                    // like a module.
                    transformModulesToAmd = 'auto';
                }
            }
            return js_transform_1.jsTransform(content, {
                compile: getCompileTarget(file, jsOptions),
                externalHelpers: true,
                minify: shouldMinifyFile(file),
                moduleResolution: jsOptions.moduleResolution,
                filePath: file.path,
                rootDir: options.rootDir,
                transformModulesToAmd,
            });
        };
        super('js-transform', transformer);
    }
}
exports.JsTransform = JsTransform;
/**
 * Transform HTML.
 */
class HtmlTransform extends GenericOptimizeTransform {
    constructor(options) {
        const jsOptions = options.js || {};
        const shouldMinifyFile = options.html && options.html.minify ?
            notExcluded(options.html.minify) :
            () => false;
        const transformer = (content, file) => {
            const transformModulesToAmd = options.js && options.js.transformModulesToAmd;
            const isEntryPoint = !!options.entrypointPath && file.path === options.entrypointPath;
            let injectBabelHelpers = 'none';
            let injectRegeneratorRuntime = false;
            if (isEntryPoint) {
                const compileTarget = getCompileTarget(file, jsOptions);
                switch (compileTarget) {
                    case 'es5':
                    case true:
                        injectBabelHelpers = 'full';
                        injectRegeneratorRuntime = true;
                        break;
                    case 'es2015':
                    case 'es2016':
                    case 'es2017':
                        injectBabelHelpers = 'full';
                        injectRegeneratorRuntime = false;
                        break;
                    case 'es2018':
                    case false:
                        injectBabelHelpers = transformModulesToAmd ? 'amd' : 'none';
                        injectRegeneratorRuntime = false;
                        break;
                    default:
                        const never = compileTarget;
                        throw new Error(`Unexpected compile target ${never}`);
                }
            }
            return html_transform_1.htmlTransform(content, {
                js: {
                    transformModulesToAmd,
                    externalHelpers: true,
                },
                minifyHtml: shouldMinifyFile(file),
                injectBabelHelpers,
                injectRegeneratorRuntime,
                injectAmdLoader: isEntryPoint && transformModulesToAmd,
            });
        };
        super('html-transform', transformer);
    }
}
exports.HtmlTransform = HtmlTransform;
/**
 * CSSMinifyTransform minifies CSS that pass through it (via css-slam).
 */
class CSSMinifyTransform extends GenericOptimizeTransform {
    constructor(options) {
        super('css-slam-minify', cssSlam.css);
        this.options = options;
    }
    _transform(file, encoding, callback) {
        // css-slam will only be run if the `stripWhitespace` option is true.
        if (this.options.stripWhitespace) {
            super._transform(file, encoding, callback);
        }
    }
}
exports.CSSMinifyTransform = CSSMinifyTransform;
/**
 * InlineCSSOptimizeTransform minifies inlined CSS (found in HTML files) that
 * passes through it (via css-slam).
 */
class InlineCSSOptimizeTransform extends GenericOptimizeTransform {
    constructor(options) {
        super('css-slam-inline', cssSlam.html);
        this.options = options;
    }
    _transform(file, encoding, callback) {
        // css-slam will only be run if the `stripWhitespace` option is true.
        if (this.options.stripWhitespace) {
            super._transform(file, encoding, callback);
        }
    }
}
exports.InlineCSSOptimizeTransform = InlineCSSOptimizeTransform;
/**
 * Returns an array of optimization streams to use in your build, based on the
 * OptimizeOptions given.
 */
function getOptimizeStreams(options) {
    options = options || {};
    const streams = [];
    streams.push(gulpif(matchesExt('.js'), new JsTransform(options)));
    streams.push(gulpif(matchesExt('.html'), new HtmlTransform(options)));
    if (options.css && options.css.minify) {
        streams.push(gulpif(matchesExtAndNotExcluded('.css', options.css.minify), new CSSMinifyTransform({ stripWhitespace: true })));
        // TODO(fks): Remove this InlineCSSOptimizeTransform stream once CSS
        // is properly being isolated by splitHtml() & rejoinHtml().
        streams.push(gulpif(matchesExtAndNotExcluded('.html', options.css.minify), new InlineCSSOptimizeTransform({ stripWhitespace: true })));
    }
    return streams;
}
exports.getOptimizeStreams = getOptimizeStreams;
function matchesExt(extension) {
    return (fs) => !!fs.path && fs.relative.endsWith(extension);
}
exports.matchesExt = matchesExt;
function notExcluded(option) {
    const exclude = typeof option === 'object' && option.exclude || [];
    return (fs) => !exclude.some((pattern) => matcher.isMatch(fs.relative, pattern));
}
function matchesExtAndNotExcluded(extension, option) {
    const a = matchesExt(extension);
    const b = notExcluded(option);
    return (fs) => a(fs) && b(fs);
}
//# sourceMappingURL=optimize-streams.js.map