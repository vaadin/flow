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
import { SourceRange } from '../model/model';
import { ParsedDocument, StringifyOptions } from '../parser/document';
export declare type Json = JsonObject | JsonArray | number | string | boolean | null;
export interface JsonObject {
    [key: string]: Json;
}
export interface JsonArray extends Array<Json> {
}
export interface Visitor {
    visit(node: Json): void;
}
export declare class ParsedJsonDocument extends ParsedDocument<Json, Visitor> {
    readonly type = "json";
    visit(visitors: Visitor[]): void;
    private _visit;
    protected _sourceRangeForNode(_node: Json): SourceRange;
    stringify(options?: StringifyOptions): string;
}
