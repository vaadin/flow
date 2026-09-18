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
import { Severity, Warning } from '../model/model';
import { IMinimatch } from 'minimatch';
export interface Options {
    /**
     * Warning codes like 'parse-error' or 'behavior-not-found' to filter out.
     */
    warningCodesToIgnore?: ReadonlySet<string>;
    /**
     * All warnings below this level of severity will be filtered out.
     */
    minimumSeverity: Severity;
    /**
     * Any file whose URL matches one of these minimatch globs will be ignored.
     */
    filesToIgnore?: ReadonlyArray<string>;
}
export declare class WarningFilter {
    warningCodesToIgnore: ReadonlySet<string>;
    minimumSeverity: Severity;
    fileGlobsToFilterOut: ReadonlyArray<IMinimatch>;
    constructor(options: Options);
    shouldIgnore(warning: Warning): boolean;
}
