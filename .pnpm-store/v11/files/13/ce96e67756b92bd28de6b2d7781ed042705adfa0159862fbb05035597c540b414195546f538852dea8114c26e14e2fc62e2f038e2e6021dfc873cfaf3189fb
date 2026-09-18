"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
const common_1 = require("./common");
const node_visitor_1 = require("./node-visitor");
/**
 * Class that implements basic stringification of an AST produced by the Parser.
 */
class Stringifier extends node_visitor_1.NodeVisitor {
    /**
     * Stringify an AST such as one produced by a Parser.
     * @param ast A node object representing the root of an AST.
     * @return The stringified CSS corresponding to the AST.
     */
    stringify(ast) {
        return this.visit(ast) || '';
    }
    /**
     * Visit and stringify a Stylesheet node.
     * @param stylesheet A Stylesheet node.
     * @return The stringified CSS of the Stylesheet.
     */
    [common_1.nodeType.stylesheet](stylesheet) {
        let rules = '';
        for (let i = 0; i < stylesheet.rules.length; ++i) {
            rules += this.visit(stylesheet.rules[i]);
        }
        return rules;
    }
    /**
     * Visit and stringify an At Rule node.
     * @param atRule An At Rule node.
     * @return The stringified CSS of the At Rule.
     */
    [common_1.nodeType.atRule](atRule) {
        return `@${atRule.name}` +
            (atRule.parameters ? ` ${atRule.parameters}` : '') +
            (atRule.rulelist ? `${this.visit(atRule.rulelist)}` : ';');
    }
    /**
     * Visit and stringify a Rulelist node.
     * @param rulelist A Rulelist node.
     * @return The stringified CSS of the Rulelist.
     */
    [common_1.nodeType.rulelist](rulelist) {
        let rules = '{';
        for (let i = 0; i < rulelist.rules.length; ++i) {
            rules += this.visit(rulelist.rules[i]);
        }
        return rules + '}';
    }
    /**
     * Visit and stringify a Comment node.
     * @param comment A Comment node.
     * @return The stringified CSS of the Comment.
     */
    [common_1.nodeType.comment](comment) {
        return `${comment.value}`;
    }
    /**
     * Visit and stringify a Seletor node.
     * @param ruleset A Ruleset node.
     * @return The stringified CSS of the Ruleset.
     */
    [common_1.nodeType.ruleset](ruleset) {
        return `${ruleset.selector}${this.visit(ruleset.rulelist)}`;
    }
    /**
     * Visit and stringify a Declaration node.
     * @param declaration A Declaration node.
     * @return The stringified CSS of the Declaration.
     */
    [common_1.nodeType.declaration](declaration) {
        return declaration.value != null ?
            `${declaration.name}:${this.visit(declaration.value)};` :
            `${declaration.name};`;
    }
    /**
     * Visit and stringify an Expression node.
     * @param expression An Expression node.
     * @return The stringified CSS of the Expression.
     */
    [common_1.nodeType.expression](expression) {
        return `${expression.text}`;
    }
    /**
     * Visit a discarded node.
     * @param discarded A Discarded node.
     * @return An empty string, since Discarded nodes are discarded.
     */
    [common_1.nodeType.discarded](_discarded) {
        return '';
    }
}
exports.Stringifier = Stringifier;
//# sourceMappingURL=stringifier.js.map