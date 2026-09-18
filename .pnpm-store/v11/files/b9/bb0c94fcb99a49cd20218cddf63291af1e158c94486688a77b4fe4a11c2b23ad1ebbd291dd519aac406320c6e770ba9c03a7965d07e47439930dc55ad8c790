/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
import * as babel from '@babel/types';
import * as doctrine from 'doctrine';
import { MethodParam, ScannedMethod, ScannedProperty } from '../index';
import { Result } from '../model/analysis';
import { ImmutableSet } from '../model/immutable';
import { Privacy } from '../model/model';
import { ScannedEvent, SourceRange, Warning } from '../model/model';
import { ParsedDocument } from '../parser/document';
import { JavaScriptDocument } from './javascript-document';
import * as jsdoc from './jsdoc';
/**
 * Returns whether a Babel node matches a particular object path.
 *
 * e.g. you have a MemberExpression node, and want to see whether it represents
 * `Foo.Bar.Baz`:
 *    matchesCallExpressio
    (node, ['Foo', 'Bar', 'Baz'])
 *
 * @param {babel.Node} expression The Babel node to match against.
 * @param {Array<string>} path The path to look for.
 */
export declare function matchesCallExpression(expression: babel.MemberExpression, path: string[]): boolean;
export declare type PropertyOrMethod = babel.ObjectProperty | babel.ObjectMethod | babel.ClassMethod | babel.AssignmentProperty;
/**
 * Given a property or method, return its name, or undefined if that name can't
 * be determined.
 */
export declare function getPropertyName(prop: PropertyOrMethod): string | undefined;
/**
 * Yields properties and methods, filters out spread expressions or anything
 * else.
 */
export declare function getSimpleObjectProperties(node: babel.ObjectExpression): IterableIterator<babel.ObjectMember>;
/** Like getSimpleObjectProperties but deals with paths. */
export declare function getSimpleObjectPropPaths(nodePath: NodePath<babel.ObjectExpression>): IterableIterator<NodePath<babel.ObjectMethod> | NodePath<babel.ObjectProperty>>;
export declare const CLOSURE_CONSTRUCTOR_MAP: Map<string, string>;
/**
 * AST expression -> Closure type.
 *
 * Accepts literal values, and native constructors.
 *
 * @param {Node} node A Babel expression node.
 * @return {string} The type of that expression, in Closure terms.
 */
export declare function getClosureType(node: babel.Node, parsedJsdoc: doctrine.Annotation | undefined, sourceRange: SourceRange, document: ParsedDocument): Result<string, Warning>;
/**
 * Tries to find the comment for the given node.
 *
 * Will look up the tree at comments on parents as appropriate, but should
 * not look at unrelated nodes. Stops at the nearest statement boundary.
 */
export declare function getBestComment(nodePath: NodePath): string | undefined;
export declare function getAttachedComment(node: babel.Node): string | undefined;
/**
 * Returns all comments from a tree defined with @event.
 */
export declare function getEventComments(node: babel.Node): Map<string, ScannedEvent>;
export declare function getPropertyValue(node: babel.ObjectExpression, name: string): babel.Node | undefined;
/**
 * Create a ScannedMethod object from an estree Property AST node.
 */
export declare function toScannedMethod(node: babel.ObjectProperty | babel.ObjectMethod | babel.ClassMethod, sourceRange: SourceRange, document: JavaScriptDocument): ScannedMethod;
export declare function getReturnFromAnnotation(jsdocAnn: jsdoc.Annotation): {
    type?: string;
    desc?: string;
} | undefined;
/**
 * Examine the body of a function to see if we can infer something about its
 * return type. This currently only handles the case where a function definitely
 * returns void.
 */
export declare function inferReturnFromBody(node: babel.Function): {
    type: string;
} | undefined;
export declare function toMethodParam(nodeParam: babel.LVal, jsdocAnn?: jsdoc.Annotation): MethodParam;
export declare function getOrInferPrivacy(name: string, annotation: jsdoc.Annotation | undefined, defaultPrivacy?: Privacy): Privacy;
/**
 * Properties on element prototypes that are part of the custom elment
 * lifecycle or Polymer configuration syntax.
 *
 * TODO(rictic): only treat the Polymer ones as private when dealing with
 *   Polymer.
 */
export declare const configurationProperties: ImmutableSet<string>;
/**
 * Scan any methods on the given node, if it's a class expression/declaration.
 */
export declare function getMethods(node: babel.Node, document: JavaScriptDocument): Map<string, ScannedMethod>;
export declare function getConstructorMethod(astNode: babel.Node, document: JavaScriptDocument): ScannedMethod | undefined;
export declare function getConstructorClassMethod(astNode: babel.Class): babel.ClassMethod | undefined;
/**
 * Scan any static methods on the given node, if it's a class
 * expression/declaration.
 */
export declare function getStaticMethods(node: babel.Node, document: JavaScriptDocument): Map<string, ScannedMethod>;
export declare function extractPropertyFromGetterOrSetter(method: babel.ClassMethod | babel.ObjectMethod, jsdocAnn: jsdoc.Annotation | undefined, document: JavaScriptDocument): ScannedProperty | null;
/**
 * Extracts properties (including accessors) from a given class
 * or object expression.
 */
export declare function extractPropertiesFromClassOrObjectBody(node: babel.Class | babel.ObjectExpression, document: JavaScriptDocument): Map<string, ScannedProperty>;
/**
 * Get the canonical statement or declaration for the given node.
 *
 * It would otherwise be difficult, or require specialized code for each kind of
 * feature, to determine which node is the canonical node for a feature. This
 * function is simple, it only walks up, and it stops once it reaches a clear
 * feature boundary. And since we're calling this function both on the indexing
 * and the lookup sides, we can be confident that both will agree on the same
 * node.
 *
 * There may be more than one feature within a single statement (e.g. `export
 * class Foo {}` is both a Class and an Export, but between `kind` and `id` we
 * should still have enough info to narrow down to the intended feature.
 *
 * See `DeclaredWithStatement` and `BaseDocumentQuery` to see where this is
 * used.
 */
export declare function getCanonicalStatement(nodePath: NodePath): babel.Statement | undefined;
/** What names does a declaration assign to? */
export declare function getBindingNamesFromDeclaration(declaration: babel.Declaration | null | undefined): IterableIterator<string>;
