/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
import { NodePath } from '@babel/traverse';
import { ExportAllDeclaration, ExportNamedDeclaration, ImportDeclaration, Program } from 'babel-types';
declare type HasSpecifier = ImportDeclaration | ExportNamedDeclaration | ExportAllDeclaration;
/**
 * Rewrites so-called "bare module specifiers" to be web-compatible paths.
 */
export declare const resolveBareSpecifiers: (filePath: string, isComponentRequest: boolean, packageName?: string | undefined, componentDir?: string | undefined, rootDir?: string | undefined) => {
    inherits: any;
    visitor: {
        Program(path: NodePath<Program>): void;
        'ImportDeclaration|ExportNamedDeclaration|ExportAllDeclaration'(path: NodePath<HasSpecifier>): void;
    };
};
export {};
