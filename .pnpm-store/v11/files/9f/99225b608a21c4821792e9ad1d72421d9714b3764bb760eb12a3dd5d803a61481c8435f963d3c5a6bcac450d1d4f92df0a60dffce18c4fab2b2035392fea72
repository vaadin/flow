/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
import * as ts from './ts-ast';
/**
 * Convert a Closure type expression string to its equivalent TypeScript AST
 * node.
 *
 * Note that function and method parameters should instead use
 * `closureParamToTypeScript`.
 */
export declare function closureTypeToTypeScript(closureType: string | null | undefined, templateTypes?: string[]): ts.Type;
/**
 * Convert a Closure function or method parameter type expression string to its
 * equivalent TypeScript AST node.
 *
 * This differs from `closureTypeToTypeScript` in that it always returns a
 * `ParamType`, and can parse the optional (`foo=`) and rest (`...foo`)
 * syntaxes, which only apply when parsing an expression in the context of a
 * parameter.
 */
export declare function closureParamToTypeScript(name: string, closureType: string | null | undefined, templateTypes?: string[]): ts.ParamType;
