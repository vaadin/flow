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
import { PrototypeMemberFinder } from '../javascript/class-scanner';
import { Visitor } from '../javascript/estree-visitor';
import { JavaScriptDocument } from '../javascript/javascript-document';
import * as jsdoc from '../javascript/jsdoc';
import { Warning } from '../model/model';
import { ScannedPolymerElementMixin } from './polymer-element-mixin';
export declare class MixinVisitor implements Visitor {
    mixins: ScannedPolymerElementMixin[];
    private _document;
    private _currentMixin;
    private _currentMixinNode;
    private _currentMixinFunction;
    private _prototypeMemberFinder;
    readonly warnings: Warning[];
    constructor(document: JavaScriptDocument, prototypeMemberFinder: PrototypeMemberFinder);
    enterAssignmentExpression(node: babel.AssignmentExpression, _parent: babel.Node, path: NodePath): void;
    enterFunctionDeclaration(node: babel.FunctionDeclaration, _parent: babel.Node, path: NodePath): void;
    leaveFunctionDeclaration(node: babel.FunctionDeclaration, _parent: babel.Node): void;
    enterVariableDeclaration(node: babel.VariableDeclaration, _parent: babel.Node, path: NodePath): void;
    leaveVariableDeclaration(node: babel.VariableDeclaration, _parent: babel.Node): void;
    private tryInitializeMixin;
    private clearOnLeave;
    enterFunctionExpression(node: babel.FunctionExpression, _parent: babel.Node): void;
    enterArrowFunctionExpression(node: babel.ArrowFunctionExpression, _parent: babel.Node): void;
    enterClassExpression(node: babel.ClassExpression, parent: babel.Node): void;
    enterClassDeclaration(node: babel.ClassDeclaration, _parent: babel.Node): void;
    private _handleClass;
}
export declare function hasMixinFunctionDocTag(docs: jsdoc.Annotation): boolean;
export declare function hasMixinClassDocTag(docs: jsdoc.Annotation): boolean;
