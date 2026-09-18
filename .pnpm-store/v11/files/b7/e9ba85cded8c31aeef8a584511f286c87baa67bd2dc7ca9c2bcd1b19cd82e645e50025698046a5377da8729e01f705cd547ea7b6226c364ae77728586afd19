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
/// <reference types="node" />
import * as chalk from 'chalk';
import { Warning, WarningStringifyOptions } from '../model/model';
export declare type Verbosity = 'one-line' | 'full';
export declare class WarningPrinter {
    private _outStream;
    _chalk: chalk.Chalk;
    private _options;
    constructor(_outStream: NodeJS.WritableStream, options?: Partial<WarningStringifyOptions>);
    /**
     * Convenience method around `printWarning`.
     */
    printWarnings(warnings: Iterable<Warning>): Promise<void>;
}
