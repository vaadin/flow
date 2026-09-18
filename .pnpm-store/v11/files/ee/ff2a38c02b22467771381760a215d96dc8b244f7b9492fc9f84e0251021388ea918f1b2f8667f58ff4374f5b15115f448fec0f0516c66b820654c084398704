/// <reference types="node" />
/// <reference types="vinyl" />
import * as stream from 'stream';
import * as vinyl from 'vinyl';
/**
 * Transforms all inline styles in `html` with `filter`
 */
export declare function html(text: string): string;
export declare function css(text: string): string;
export declare class GulpTransform extends stream.Transform {
    constructor();
    _transform(file: vinyl, _encoding: string, callback: (error?: Error, file?: vinyl) => void): void;
}
export declare function gulp(): GulpTransform;
