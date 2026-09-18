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

import * as cssSlam from 'css-slam';
import * as gulpif from 'gulp-if';
import * as logging from 'plylog';
import {JsCompileTarget, ModuleResolutionStrategy} from 'polymer-project-config';
import {Transform} from 'stream';
import * as vinyl from 'vinyl';

import matcher = require('matcher');

import {jsTransform} from './js-transform';
import {htmlTransform} from './html-transform';
import {isHtmlSplitterFile} from './html-splitter';

// TODO(fks) 09-22-2016: Latest npm type declaration resolves to a non-module
// entity. Upgrade to proper JS import once compatible .d.ts file is released,
// or consider writing a custom declaration in the `custom_typings/` folder.
import File = require('vinyl');

const logger = logging.getLogger('cli.build.optimize-streams');

export type FileCB = (error?: Error, file?: File) => void;
export type CSSOptimizeOptions = {
  stripWhitespace?: boolean;
};
export interface OptimizeOptions {
  html?: {
    minify?: boolean|{exclude?: string[]},
  };
  css?: {
    minify?: boolean|{exclude?: string[]},
  };
  js?: JsOptimizeOptions;
  entrypointPath?: string;
  rootDir?: string;
}

export type JsCompileOptions = boolean|JsCompileTarget|{
  target?: JsCompileTarget;
  exclude?: string[];
};

export interface JsOptimizeOptions {
  minify?: boolean|{exclude?: string[]};
  compile?: JsCompileOptions;
  moduleResolution?: ModuleResolutionStrategy;
  transformModulesToAmd?: boolean;
}

/**
 * GenericOptimizeTransform is a generic optimization stream. It can be extended
 * to create a new kind of specific file-type optimizer, or it can be used
 * directly to create an ad-hoc optimization stream for different libraries.
 * If the transform library throws an exception when run, the file will pass
 * through unaffected.
 */
export class GenericOptimizeTransform extends Transform {
  optimizer: (content: string, file: File) => string;
  optimizerName: string;

  constructor(
      optimizerName: string,
      optimizer: (content: string, file: File) => string) {
    super({objectMode: true});
    this.optimizer = optimizer;
    this.optimizerName = optimizerName;
  }

  _transform(file: File, _encoding: string, callback: FileCB): void {
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
      } catch (error) {
        logger.warn(
            `${this.optimizerName}: Unable to optimize ${file.path}`,
            {err: error.message || error});
      }
    }
    callback(undefined, file);
  }
}

function getCompileTarget(
    file: vinyl, options: JsOptimizeOptions): JsCompileTarget|boolean {
  let target: JsCompileTarget|boolean|undefined;
  const compileOptions = options.compile;
  if (notExcluded(options.compile)(file)) {
    if (typeof compileOptions === 'object') {
      target =
          (compileOptions.target === undefined) ? true : compileOptions.target;
    } else {
      target = compileOptions;
    }
    if (target === undefined) {
      target = false;
    }
  } else {
    target = false;
  }
  return target;
}

/**
 * Transform JavaScript.
 */
export class JsTransform extends GenericOptimizeTransform {
  constructor(options: OptimizeOptions) {
    const jsOptions: JsOptimizeOptions = options.js || {};

    const shouldMinifyFile =
        jsOptions.minify ? notExcluded(jsOptions.minify) : () => false;

    const transformer = (content: string, file: File) => {
      let transformModulesToAmd: boolean|'auto' = false;

      if (jsOptions.transformModulesToAmd) {
        if (isHtmlSplitterFile(file)) {
          // This is a type=module script in an HTML file. Definitely AMD
          // transform.
          transformModulesToAmd = file.isModule === true;
        } else {
          // This is an external script file. Only AMD transform it if it looks
          // like a module.
          transformModulesToAmd = 'auto';
        }
      }

      return jsTransform(content, {
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

/**
 * Transform HTML.
 */
export class HtmlTransform extends GenericOptimizeTransform {
  constructor(options: OptimizeOptions) {
    const jsOptions: JsOptimizeOptions = options.js || {};

    const shouldMinifyFile = options.html && options.html.minify ?
        notExcluded(options.html.minify) :
        () => false;

    const transformer = (content: string, file: File) => {
      const transformModulesToAmd =
          options.js && options.js.transformModulesToAmd;
      const isEntryPoint =
          !!options.entrypointPath && file.path === options.entrypointPath;

      let injectBabelHelpers: 'none'|'full'|'amd' = 'none';
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
            const never: never = compileTarget;
            throw new Error(`Unexpected compile target ${never}`);
        }
      }

      return htmlTransform(content, {
        js: {
          transformModulesToAmd,
          externalHelpers: true,
          // Note we don't do any other JS transforms here (like compilation),
          // because we're assuming that HtmlSplitter has run and any inline
          // scripts will be compiled in their own stream.
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

/**
 * CSSMinifyTransform minifies CSS that pass through it (via css-slam).
 */
export class CSSMinifyTransform extends GenericOptimizeTransform {
  constructor(private options: CSSOptimizeOptions) {
    super('css-slam-minify', cssSlam.css);
  }

  _transform(file: File, encoding: string, callback: FileCB): void {
    // css-slam will only be run if the `stripWhitespace` option is true.
    if (this.options.stripWhitespace) {
      super._transform(file, encoding, callback);
    }
  }
}

/**
 * InlineCSSOptimizeTransform minifies inlined CSS (found in HTML files) that
 * passes through it (via css-slam).
 */
export class InlineCSSOptimizeTransform extends GenericOptimizeTransform {
  constructor(private options: CSSOptimizeOptions) {
    super('css-slam-inline', cssSlam.html);
  }

  _transform(file: File, encoding: string, callback: FileCB): void {
    // css-slam will only be run if the `stripWhitespace` option is true.
    if (this.options.stripWhitespace) {
      super._transform(file, encoding, callback);
    }
  }
}

/**
 * Returns an array of optimization streams to use in your build, based on the
 * OptimizeOptions given.
 */
export function getOptimizeStreams(options?: OptimizeOptions):
    NodeJS.ReadWriteStream[] {
  options = options || {};
  const streams = [];

  streams.push(gulpif(matchesExt('.js'), new JsTransform(options)));
  streams.push(gulpif(matchesExt('.html'), new HtmlTransform(options)));

  if (options.css && options.css.minify) {
    streams.push(gulpif(
        matchesExtAndNotExcluded('.css', options.css.minify),
        new CSSMinifyTransform({stripWhitespace: true})));
    // TODO(fks): Remove this InlineCSSOptimizeTransform stream once CSS
    // is properly being isolated by splitHtml() & rejoinHtml().
    streams.push(gulpif(
        matchesExtAndNotExcluded('.html', options.css.minify),
        new InlineCSSOptimizeTransform({stripWhitespace: true})));
  }

  return streams;
}

export function matchesExt(extension: string) {
  return (fs: vinyl) => !!fs.path && fs.relative.endsWith(extension);
}

function notExcluded(option?: JsCompileOptions) {
  const exclude = typeof option === 'object' && option.exclude || [];
  return (fs: vinyl) => !exclude.some(
             (pattern: string) => matcher.isMatch(fs.relative, pattern));
}

function matchesExtAndNotExcluded(extension: string, option: JsCompileOptions) {
  const a = matchesExt(extension);
  const b = notExcluded(option);
  return (fs: vinyl) => a(fs) && b(fs);
}
