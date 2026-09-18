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

import dynamicImportSyntax from '@babel/plugin-syntax-dynamic-import';
import {NodePath} from '@babel/traverse';
import {CallExpression, ExportAllDeclaration, ExportNamedDeclaration, ImportDeclaration, Program} from 'babel-types';
import {resolve} from 'polymer-analyzer/lib/javascript/resolve-specifier-node';

const isPathSpecifier = (s: string) => /^\.{0,2}\//.test(s);

type HasSpecifier =
    ImportDeclaration|ExportNamedDeclaration|ExportAllDeclaration;

/**
 * Rewrites so-called "bare module specifiers" to be web-compatible paths.
 */
export const resolveBareSpecifiers = (
    filePath: string,
    isComponentRequest: boolean,
    packageName?: string,
    componentDir?: string,
    rootDir?: string,
    ) => ({
  inherits: dynamicImportSyntax,

  visitor: {
    Program(path: NodePath<Program>) {
      path.traverse({
        CallExpression(path: NodePath<CallExpression>) {
          if (path.node.callee.type as string === 'Import') {
            const specifierArg = path.node.arguments[0];
            if (specifierArg.type !== 'StringLiteral') {
              // Should never happen
              return;
            }
            const specifier = specifierArg.value;
            specifierArg.value = maybeResolve(
              specifier,
              filePath,
              isComponentRequest,
              packageName,
              componentDir,
              rootDir);
          }
        }
      });
    },
    'ImportDeclaration|ExportNamedDeclaration|ExportAllDeclaration'(
        path: NodePath<HasSpecifier>) {
      const node = path.node;

      // An export without a 'from' clause
      if (node.source == null) {
        return;
      }

      const specifier = node.source.value;
      node.source.value = maybeResolve(
          specifier,
          filePath,
          isComponentRequest,
          packageName,
          componentDir,
          rootDir);
    }
  }
});

const maybeResolve = (
    specifier: string,
    filePath: string,
    isComponentRequest: boolean,
    packageName?: string,
    componentDir?: string,
    rootDir?: string,
    ) => {
  try {
    let componentInfo;
    if (isComponentRequest) {
      componentInfo = {
        packageName: packageName!,
        componentDir: componentDir!,
        rootDir: rootDir!,
      };
    }
    return resolve(specifier, filePath, componentInfo);
  } catch (e) {
    // `require` and `meta` are fake imports that our other build tooling
    // injects, so we should not warn for them.
    if (!isPathSpecifier(specifier) && specifier !== 'require' &&
        specifier !== 'meta') {
      // Don't warn if the specifier was already a path, even though we do
      // resolve paths, because maybe the user is serving it some other
      // way.
      console.warn(
          `Could not resolve module specifier "${specifier}" ` +
          `in file "${filePath}".`);
    }
    return specifier;
  }
};
