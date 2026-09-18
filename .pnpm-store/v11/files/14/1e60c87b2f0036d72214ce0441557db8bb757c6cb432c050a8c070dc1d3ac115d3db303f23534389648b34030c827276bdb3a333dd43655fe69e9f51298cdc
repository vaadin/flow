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
import * as dom5 from 'dom5/lib/index-next';
import { Transform } from 'stream';
import File = require('vinyl');
/**
 * HTMLSplitter represents the shared state of files as they are passed through
 * a splitting stream and then a rejoining stream. Creating a new instance of
 * HTMLSplitter and adding its streams to the build pipeline is the
 * supported user interface for splitting out and rejoining inlined CSS & JS in
 * the build process.
 */
export declare class HtmlSplitter {
    private _splitFiles;
    private _parts;
    /**
     * Returns a new `Transform` stream that splits inline script and styles into
     * new, separate files that are passed out of the stream.
     */
    split(): Transform;
    /**
     * Returns a new `Transform` stream that rejoins inline scripts and styles
     * that were originally split from this `HTMLSplitter`'s `split()` back into
     * their parent HTML files.
     */
    rejoin(): Transform;
    isSplitFile(parentPath: string): boolean;
    getSplitFile(parentPath: string): SplitFile;
    addSplitPath(parentPath: string, childPath: string): void;
    getParentFile(childPath: string): SplitFile | undefined;
}
/**
 * Returns whether the given script tag was an inline script that was split out
 * into a fake file by HtmlSplitter.
 */
export declare function scriptWasSplitByHtmlSplitter(script: dom5.Node): boolean;
export declare type HtmlSplitterFile = File & {
    fromHtmlSplitter?: true;
    isModule?: boolean;
};
/**
 * Return whether the given Vinyl file was created by the HtmlSplitter from an
 * HTML document script tag.
 */
export declare function isHtmlSplitterFile(file: File): file is HtmlSplitterFile;
/**
 * Represents a file that is split into multiple files.
 */
export declare class SplitFile {
    path: string;
    parts: Map<string, string | null>;
    outstandingPartCount: number;
    vinylFile: File | null;
    constructor(path: string);
    addPartPath(path: string): void;
    setPartContent(path: string, content: string): void;
    readonly isComplete: boolean;
}
