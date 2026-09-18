var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/common", ["require", "exports"], function (require, exports) {
    "use strict";
    /**
     * A set of common RegExp matchers for tokenizing CSS.
     * @constant
     * @type {object}
     * @default
     */
    var matcher = {
        whitespace: /\s/,
        whitespaceGreedy: /(\s+)/g,
        commentGreedy: /(\*\/)/g,
        boundary: /[\(\)\{\}'"@;:\s]/,
        stringBoundary: /['"]/
    };
    exports.matcher = matcher;
    /**
     * An enumeration of Node types.
     * @constant
     * @type {object}
     * @default
     */
    var nodeType = {
        stylesheet: 'stylesheet',
        comment: 'comment',
        atRule: 'atRule',
        ruleset: 'ruleset',
        expression: 'expression',
        declaration: 'declaration',
        rulelist: 'rulelist',
        discarded: 'discarded'
    };
    exports.nodeType = nodeType;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/token", ["require", "exports"], function (require, exports) {
    "use strict";
    /**
     * An enumeration of Token types.
     * @type {object}
     * @default
     * @static
     */
    var TokenType;
    (function (TokenType) {
        TokenType[TokenType["none"] = 0] = "none";
        TokenType[TokenType["whitespace"] = (Math.pow(2, 0))] = "whitespace";
        TokenType[TokenType["string"] = (Math.pow(2, 1))] = "string";
        TokenType[TokenType["comment"] = (Math.pow(2, 2))] = "comment";
        TokenType[TokenType["word"] = (Math.pow(2, 3))] = "word";
        TokenType[TokenType["boundary"] = (Math.pow(2, 4))] = "boundary";
        TokenType[TokenType["propertyBoundary"] = (Math.pow(2, 5))] = "propertyBoundary";
        // Special cases for boundary:
        TokenType[TokenType["openParenthesis"] = (Math.pow(2, 6)) | TokenType.boundary] = "openParenthesis";
        TokenType[TokenType["closeParenthesis"] = (Math.pow(2, 7)) | TokenType.boundary] = "closeParenthesis";
        TokenType[TokenType["at"] = (Math.pow(2, 8)) | TokenType.boundary] = "at";
        TokenType[TokenType["openBrace"] = (Math.pow(2, 9)) | TokenType.boundary] = "openBrace";
        // [};] are property boundaries:
        TokenType[TokenType["closeBrace"] = (Math.pow(2, 10)) | TokenType.propertyBoundary | TokenType.boundary] = "closeBrace";
        TokenType[TokenType["semicolon"] = (Math.pow(2, 11)) | TokenType.propertyBoundary | TokenType.boundary] = "semicolon";
        // : is a chimaeric abomination:
        // foo:bar{}
        // foo:bar;
        TokenType[TokenType["colon"] = (Math.pow(2, 12)) | TokenType.boundary | TokenType.word] = "colon";
        // TODO: are these two boundaries? I mean, sometimes they are I guess? Or
        //       maybe they shouldn't exist in the boundaryTokenTypes map.
        TokenType[TokenType["hyphen"] = (Math.pow(2, 13))] = "hyphen";
        TokenType[TokenType["underscore"] = (Math.pow(2, 14))] = "underscore";
    })(TokenType || (TokenType = {}));
    ;
    /**
     * Class that describes individual tokens as produced by the Tokenizer.
     */
    var Token = (function () {
        /**
         * Create a Token instance.
         * @param {number} type The lexical type of the Token.
         * @param {number} start The start index of the text corresponding to the
         * Token in the CSS text.
         * @param {number} end The end index of the text corresponding to the Token
         * in the CSS text.
         */
        function Token(type, start, end) {
            if (start === void 0) { start = undefined; }
            if (end === void 0) { end = undefined; }
            this.type = type;
            this.start = start;
            this.end = end;
            this.previous = null;
            this.next = null;
        }
        /**
         * Test if the Token matches a given numeric type. Types match if the bitwise
         * AND of the Token's type and the argument type are equivalent to the
         * argument type.
         * @param {number} type The numeric type to test for equivalency with the
         * Token.
         */
        Token.prototype.is = function (type) {
            return (this.type & type) === type;
        };
        return Token;
    }());
    Token.type = TokenType;
    exports.Token = Token;
    /**
     * A mapping of boundary token text to their corresponding types.
     * @type {object}
     * @default
     * @const
     */
    var boundaryTokenTypes = {
        '(': Token.type.openParenthesis,
        ')': Token.type.closeParenthesis,
        ':': Token.type.colon,
        '@': Token.type.at,
        '{': Token.type.openBrace,
        '}': Token.type.closeBrace,
        ';': Token.type.semicolon,
        '-': Token.type.hyphen,
        '_': Token.type.underscore
    };
    exports.boundaryTokenTypes = boundaryTokenTypes;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/tokenizer", ["require", "exports", "shady-css/common", "shady-css/token"], function (require, exports, common_1, token_1) {
    "use strict";
    var currentToken = Symbol('currentToken');
    var cursorToken = Symbol('cursorToken');
    var getNextToken = Symbol('getNextToken');
    /**
     * Class that implements tokenization of significant lexical features of the
     * CSS syntax.
     */
    var Tokenizer = (function () {
        /**
         * Create a Tokenizer instance.
         * @param {string} cssText The raw CSS string to be tokenized.
         *
         */
        function Tokenizer(cssText) {
            this.cssText = cssText;
            /**
             * Tracks the position of the tokenizer in the source string.
             * Also the default head of the Token linked list.
             * @type {!Token}
             * @private
             */
            this[cursorToken] = new token_1.Token(token_1.Token.type.none, 0, 0);
            /**
             * Holds a reference to a Token that is "next" in the source string, often
             * due to having been peeked at.
             * @type {?Token}
             * @readonly
             */
            this[currentToken] = null;
        }
        Object.defineProperty(Tokenizer.prototype, "offset", {
            get: function () {
                return this[cursorToken].end;
            },
            enumerable: true,
            configurable: true
        });
        Object.defineProperty(Tokenizer.prototype, "currentToken", {
            /**
             * The current token that will be returned by a call to `advance`. This
             * reference is useful for "peeking" at the next token ahead in the sequence.
             * If the entire CSS text has been tokenized, the `currentToken` will be null.
             * @type {Token}
             */
            get: function () {
                if (this[currentToken] == null) {
                    this[currentToken] = this[getNextToken]();
                }
                return this[currentToken];
            },
            enumerable: true,
            configurable: true
        });
        /**
         * Advance the Tokenizer to the next token in the sequence.
         * @return {Token} The current token prior to the call to `advance`, or null
         * if the entire CSS text has been tokenized.
         */
        Tokenizer.prototype.advance = function () {
            var token;
            if (this[currentToken] != null) {
                token = this[currentToken];
                this[currentToken] = null;
            }
            else {
                token = this[getNextToken]();
            }
            return token;
        };
        /**
         * Extract a slice from the CSS text, using two tokens to represent the range
         * of text to be extracted. The extracted text will include all text between
         * the start index of the first token and the offset index of the second token
         * (or the offset index of the first token if the second is not provided).
         * @param {Token} startToken The token that represents the beginning of the
         * text range to be extracted.
         * @param {Token} endToken The token that represents the end of the text range
         * to be extracted. Defaults to the startToken if no endToken is provided.
         * @return {string} The substring of the CSS text corresponding to the
         * startToken and endToken.
         */
        Tokenizer.prototype.slice = function (startToken, endToken) {
            endToken = endToken || startToken;
            return this.cssText.substring(startToken.start, endToken.end);
        };
        /**
         * Flush all tokens from the Tokenizer.
         * @return {array} An array of all tokens corresponding to the CSS text.
         */
        Tokenizer.prototype.flush = function () {
            var tokens = [];
            while (this.currentToken) {
                tokens.push(this.advance());
            }
            return tokens;
        };
        /**
         * Extract the next token from the CSS text and advance the Tokenizer.
         * @return {Token} A Token instance, or null if the entire CSS text has beeen
         * tokenized.
         */
        Tokenizer.prototype[getNextToken] = function () {
            var character = this.cssText[this.offset];
            var token;
            this[currentToken] = null;
            if (this.offset >= this.cssText.length) {
                return null;
            }
            else if (common_1.matcher.whitespace.test(character)) {
                token = this.tokenizeWhitespace(this.offset);
            }
            else if (common_1.matcher.stringBoundary.test(character)) {
                token = this.tokenizeString(this.offset);
            }
            else if (character === '/' && this.cssText[this.offset + 1] === '*') {
                token = this.tokenizeComment(this.offset);
            }
            else if (common_1.matcher.boundary.test(character)) {
                token = this.tokenizeBoundary(this.offset);
            }
            else {
                token = this.tokenizeWord(this.offset);
            }
            token.previous = this[cursorToken];
            this[cursorToken].next = token;
            this[cursorToken] = token;
            return token;
        };
        /**
         * Tokenize a string starting at a given offset in the CSS text. A string is
         * any span of text that is wrapped by eclusively paired, non-escaped matching
         * quotation marks.
         * @param {number} offset An offset in the CSS text.
         * @return {Token} A string Token instance.
         */
        Tokenizer.prototype.tokenizeString = function (offset) {
            var quotation = this.cssText[offset];
            var escaped = false;
            var start = offset;
            var character;
            while (character = this.cssText[++offset]) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (character === quotation) {
                    ++offset;
                    break;
                }
                if (character === '\\') {
                    escaped = true;
                }
            }
            return new token_1.Token(token_1.Token.type.string, start, offset);
        };
        /**
         * Tokenize a word starting at a given offset in the CSS text. A word is any
         * span of text that is not whitespace, is not a string, is not a comment and
         * is not a structural delimiter (such as braces and semicolon).
         * @param {offset} number An offset in the CSS text.
         * @return {Token} A word Token instance.
         */
        Tokenizer.prototype.tokenizeWord = function (offset) {
            var start = offset;
            var character;
            // TODO(cdata): change to greedy regex match?
            while ((character = this.cssText[offset]) &&
                !common_1.matcher.boundary.test(character)) {
                offset++;
            }
            return new token_1.Token(token_1.Token.type.word, start, offset);
        };
        /**
         * Tokenize whitespace starting at a given offset in the CSS text. Whitespace
         * is any span of text made up of consecutive spaces, tabs, newlines and other
         * single whitespace characters.
         * @param {offset} number An offset in the CSS text.
         * @return {Token} A whitespace Token instance.
         */
        Tokenizer.prototype.tokenizeWhitespace = function (offset) {
            var start = offset;
            common_1.matcher.whitespaceGreedy.lastIndex = offset;
            var match = common_1.matcher.whitespaceGreedy.exec(this.cssText);
            if (match != null && match.index === offset) {
                offset = common_1.matcher.whitespaceGreedy.lastIndex;
            }
            return new token_1.Token(token_1.Token.type.whitespace, start, offset);
        };
        /**
         * Tokenize a comment starting at a given offset in the CSS text. A comment is
         * any span of text beginning with the two characters / and *, and ending with
         * a matching counterpart pair of consecurtive characters (* and /).
         * @param {offset} number An offset in the CSS text.
         * @return {Token} A comment Token instance.
         */
        Tokenizer.prototype.tokenizeComment = function (offset) {
            var start = offset;
            common_1.matcher.commentGreedy.lastIndex = offset;
            var match = common_1.matcher.commentGreedy.exec(this.cssText);
            if (match == null) {
                offset = this.cssText.length;
            }
            else {
                offset = common_1.matcher.commentGreedy.lastIndex;
            }
            return new token_1.Token(token_1.Token.type.comment, start, offset);
        };
        /**
         * Tokenize a boundary at a given offset in the CSS text. A boundary is any
         * single structurally significant character. These characters include braces,
         * semicolons, the "at" symbol and others.
         * @param {offset} number An offset in the CSS text.
         * @return {Token} A boundary Token instance.
         */
        Tokenizer.prototype.tokenizeBoundary = function (offset) {
            // TODO(cdata): Evaluate if this is faster than a switch statement:
            var type = token_1.boundaryTokenTypes[this.cssText[offset]] || token_1.Token.type.boundary;
            return new token_1.Token(type, offset, offset + 1);
        };
        return Tokenizer;
    }());
    exports.Tokenizer = Tokenizer;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/node-factory", ["require", "exports", "shady-css/common"], function (require, exports, common_2) {
    "use strict";
    /**
     * Class used for generating nodes in a CSS AST. Extend this class to implement
     * visitors to different nodes while the tree is being generated, and / or
     * custom node generation.
     */
    var NodeFactory = (function () {
        function NodeFactory() {
        }
        /**
         * Creates a Stylesheet node.
         * @param {array} rules The list of rules that appear at the top
         * level of the stylesheet.
         * @return {object} A Stylesheet node.
         */
        NodeFactory.prototype.stylesheet = function (rules) {
            return { type: common_2.nodeType.stylesheet, rules: rules };
        };
        /**
         * Creates an At Rule node.
         * @param {string} name The "name" of the At Rule (e.g., `charset`)
         * @param {string} parameters The "parameters" of the At Rule (e.g., `utf8`)
         * @param {object=} rulelist The Rulelist node (if any) of the At Rule.
         * @return {object} An At Rule node.
         */
        NodeFactory.prototype.atRule = function (name, parameters, rulelist) {
            return { type: common_2.nodeType.atRule, name: name, parameters: parameters, rulelist: rulelist };
        };
        /**
         * Creates a Comment node.
         * @param {string} value The full text content of the comment, including
         * opening and closing comment signature.
         * @return {object} A Comment node.
         */
        NodeFactory.prototype.comment = function (value) {
            return { type: common_2.nodeType.comment, value: value };
        };
        /**
         * Creates a Rulelist node.
         * @param {array} rules An array of the Rule nodes found within the Ruleset.
         * @return {object} A Rulelist node.
         */
        NodeFactory.prototype.rulelist = function (rules) {
            return { type: common_2.nodeType.rulelist, rules: rules };
        };
        /**
         * Creates a Ruleset node.
         * @param {string} selector The selector that corresponds to the Selector
         * (e.g., `#foo > .bar`).
         * @param {object} rulelist The Rulelist node that corresponds to the Selector.
         * @return {object} A Selector node.
         */
        NodeFactory.prototype.ruleset = function (selector, rulelist) {
            return { type: common_2.nodeType.ruleset, selector: selector, rulelist: rulelist };
        };
        /**
         * Creates a Declaration node.
         * @param {string} name The property name of the Declaration (e.g., `color`).
         * @param {object} value Either an Expression node, or a Rulelist node, that
         * corresponds to the value of the Declaration.
         * @return {object} A Declaration node.
         */
        NodeFactory.prototype.declaration = function (name, value) {
            return { type: common_2.nodeType.declaration, name: name, value: value };
        };
        /**
         * Creates an Expression node.
         * @param {string} text The full text content of the expression (e.g.,
         * `url(img.jpg)`)
         * @return {object} An Expression node.
         */
        NodeFactory.prototype.expression = function (text) {
            return { type: common_2.nodeType.expression, text: text };
        };
        /**
         * Creates a Discarded node. Discarded nodes contain content that was not
         * parseable (usually due to typos, or otherwise unrecognized syntax).
         * @param {string} text The text content that is discarded.
         * @return {object} A Discarded node.
         */
        NodeFactory.prototype.discarded = function (text) {
            return { type: common_2.nodeType.discarded, text: text };
        };
        return NodeFactory;
    }());
    exports.NodeFactory = NodeFactory;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/node-visitor", ["require", "exports"], function (require, exports) {
    "use strict";
    var path = Symbol('path');
    /**
     * Class that implements a visitor pattern for ASTs produced by the Parser.
     * Extend the NodeVisitor class to implement useful tree traversal operations
     * such as stringification.
     */
    var NodeVisitor = (function () {
        /**
         * Create a NodeVisitor instance.
         */
        function NodeVisitor() {
            this[path] = [];
        }
        Object.defineProperty(NodeVisitor.prototype, "path", {
            /**
             * A list of nodes that corresponds to the current path through an AST being
             * visited, leading to where the currently visited node will be found.
             * @type {array}
             */
            get: function () {
                return this[path];
            },
            enumerable: true,
            configurable: true
        });
        /**
         * Visit a node. The visited node will be added to the `path` before it is
         * visited, and will be removed after it is visited. Nodes are "visited" by
         * calling a method on the NodeVisitor instance that matches the node's type,
         * if one is available on the NodeVisitor instance.
         * @param {object} node The node to be visited.
         * @return The return value of the method visiting the node, if any.
         */
        NodeVisitor.prototype.visit = function (node) {
            var result;
            if (this[node.type]) {
                this[path].push(node);
                result = this[node.type](node);
                this[path].pop();
            }
            return result;
        };
        return NodeVisitor;
    }());
    exports.NodeVisitor = NodeVisitor;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/stringifier", ["require", "exports", "shady-css/common", "shady-css/node-visitor"], function (require, exports, common_3, node_visitor_1) {
    "use strict";
    /**
     * Class that implements basic stringification of an AST produced by the Parser.
     */
    var Stringifier = (function (_super) {
        __extends(Stringifier, _super);
        function Stringifier() {
            return _super !== null && _super.apply(this, arguments) || this;
        }
        /**
         * Stringify an AST such as one produced by a Parser.
         * @param {object} ast A node object representing the root of an AST.
         * @return {string} The stringified CSS corresponding to the AST.
         */
        Stringifier.prototype.stringify = function (ast) {
            return this.visit(ast) || '';
        };
        /**
         * Visit and stringify a Stylesheet node.
         * @param {object} stylesheet A Stylesheet node.
         * @return {string} The stringified CSS of the Stylesheet.
         */
        Stringifier.prototype[common_3.nodeType.stylesheet] = function (stylesheet) {
            var rules = '';
            for (var i = 0; i < stylesheet.rules.length; ++i) {
                rules += this.visit(stylesheet.rules[i]);
            }
            return rules;
        };
        /**
         * Visit and stringify an At Rule node.
         * @param {object} atRule An At Rule node.
         * @return {string} The stringified CSS of the At Rule.
         */
        Stringifier.prototype[common_3.nodeType.atRule] = function (atRule) {
            return "@" + atRule.name +
                (atRule.parameters ? " " + atRule.parameters : '') +
                (atRule.rulelist ? "" + this.visit(atRule.rulelist) : ';');
        };
        /**
         * Visit and stringify a Rulelist node.
         * @param {object} rulelist A Rulelist node.
         * @return {string} The stringified CSS of the Rulelist.
         */
        Stringifier.prototype[common_3.nodeType.rulelist] = function (rulelist) {
            var rules = '{';
            for (var i = 0; i < rulelist.rules.length; ++i) {
                rules += this.visit(rulelist.rules[i]);
            }
            return rules + '}';
        };
        /**
         * Visit and stringify a Comment node.
         * @param {object} comment A Comment node.
         * @return {string} The stringified CSS of the Comment.
         */
        Stringifier.prototype[common_3.nodeType.comment] = function (comment) {
            return "" + comment.value;
        };
        /**
         * Visit and stringify a Seletor node.
         * @param {object} ruleset A Ruleset node.
         * @return {string} The stringified CSS of the Ruleset.
         */
        Stringifier.prototype[common_3.nodeType.ruleset] = function (ruleset) {
            return "" + ruleset.selector + this.visit(ruleset.rulelist);
        };
        /**
         * Visit and stringify a Declaration node.
         * @param {object} declaration A Declaration node.
         * @return {string} The stringified CSS of the Declaration.
         */
        Stringifier.prototype[common_3.nodeType.declaration] = function (declaration) {
            return declaration.value != null ?
                declaration.name + ":" + this.visit(declaration.value) + ";" :
                declaration.name + ";";
        };
        /**
         * Visit and stringify an Expression node.
         * @param {object} expression An Expression node.
         * @return {string} The stringified CSS of the Expression.
         */
        Stringifier.prototype[common_3.nodeType.expression] = function (expression) {
            return "" + expression.text;
        };
        /**
         * Visit a discarded node.
         * @param {object} discarded A Discarded node.
         * @return {string} An empty string, since Discarded nodes are discarded.
         */
        Stringifier.prototype[common_3.nodeType.discarded] = function (discarded) {
            return '';
        };
        return Stringifier;
    }(node_visitor_1.NodeVisitor));
    exports.Stringifier = Stringifier;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css/parser", ["require", "exports", "shady-css/tokenizer", "shady-css/token", "shady-css/node-factory"], function (require, exports, tokenizer_1, token_2, node_factory_1) {
    "use strict";
    /**
     * Class that implements a shady CSS parser.
     */
    var Parser = (function () {
        /**
         * Create a Parser instance. When creating a Parser instance, a specialized
         * NodeFactory can be supplied to implement streaming analysis and
         * manipulation of the CSS AST.
         */
        function Parser(nodeFactory) {
            if (nodeFactory === void 0) { nodeFactory = new node_factory_1.NodeFactory(); }
            this.nodeFactory = nodeFactory;
        }
        /**
         * Parse CSS and generate an AST.
         * @param {string} cssText The CSS to parse.
         * @return {object} A CSS AST containing nodes that correspond to those
         * generated by the Parser's NodeFactory.
         */
        Parser.prototype.parse = function (cssText) {
            return this.parseStylesheet(new tokenizer_1.Tokenizer(cssText));
        };
        /**
         * Consumes tokens from a Tokenizer to parse a Stylesheet node.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {object} A Stylesheet node.
         */
        Parser.prototype.parseStylesheet = function (tokenizer) {
            return this.nodeFactory.stylesheet(this.parseRules(tokenizer));
        };
        /**
         * Consumes tokens from a Tokenizer to parse a sequence of rules.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {array} A list of nodes corresponding to rules. For a parser
         * configured with a basic NodeFactory, any of Comment, AtRule, Ruleset,
         * Declaration and Discarded nodes may be present in the list.
         */
        Parser.prototype.parseRules = function (tokenizer) {
            var rules = [];
            while (tokenizer.currentToken) {
                var rule = this.parseRule(tokenizer);
                if (rule) {
                    rules.push(rule);
                }
            }
            return rules;
        };
        /**
         * Consumes tokens from a Tokenizer to parse a single rule.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {object} If the current token in the Tokenizer is whitespace,
         * returns null. Otherwise, returns the next parseable node.
         */
        Parser.prototype.parseRule = function (tokenizer) {
            // Trim leading whitespace:
            if (tokenizer.currentToken.is(token_2.Token.type.whitespace)) {
                tokenizer.advance();
                return null;
            }
            else if (tokenizer.currentToken.is(token_2.Token.type.comment)) {
                return this.parseComment(tokenizer);
            }
            else if (tokenizer.currentToken.is(token_2.Token.type.word)) {
                return this.parseDeclarationOrRuleset(tokenizer);
            }
            else if (tokenizer.currentToken.is(token_2.Token.type.propertyBoundary)) {
                return this.parseUnknown(tokenizer);
            }
            else if (tokenizer.currentToken.is(token_2.Token.type.at)) {
                return this.parseAtRule(tokenizer);
            }
            else {
                return this.parseUnknown(tokenizer);
            }
        };
        /**
         * Consumes tokens from a Tokenizer to parse a Comment node.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {object} A Comment node.
         */
        Parser.prototype.parseComment = function (tokenizer) {
            return this.nodeFactory.comment(tokenizer.slice(tokenizer.advance()));
        };
        /**
         * Consumes tokens from a Tokenizer through the next boundary token to
         * produce a Discarded node. This supports graceful recovery from many
         * malformed CSS conditions.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {object} A Discarded node.
         */
        Parser.prototype.parseUnknown = function (tokenizer) {
            var start = tokenizer.advance();
            var end;
            while (tokenizer.currentToken &&
                tokenizer.currentToken.is(token_2.Token.type.boundary)) {
                end = tokenizer.advance();
            }
            return this.nodeFactory.discarded(tokenizer.slice(start, end));
        };
        /**
         * Consumes tokens from a Tokenizer to parse an At Rule node.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {object} An At Rule node.
         */
        Parser.prototype.parseAtRule = function (tokenizer) {
            var name = '';
            var rulelist = null;
            var parametersStart = null;
            var parametersEnd = null;
            while (tokenizer.currentToken) {
                if (tokenizer.currentToken.is(token_2.Token.type.whitespace)) {
                    tokenizer.advance();
                }
                else if (!name && tokenizer.currentToken.is(token_2.Token.type.at)) {
                    // Discard the @:
                    tokenizer.advance();
                    var start = tokenizer.currentToken;
                    var end = void 0;
                    while (tokenizer.currentToken &&
                        tokenizer.currentToken.is(token_2.Token.type.word)) {
                        end = tokenizer.advance();
                    }
                    name = tokenizer.slice(start, end);
                }
                else if (tokenizer.currentToken.is(token_2.Token.type.openBrace)) {
                    rulelist = this.parseRulelist(tokenizer);
                    break;
                }
                else if (tokenizer.currentToken.is(token_2.Token.type.propertyBoundary)) {
                    tokenizer.advance();
                    break;
                }
                else {
                    if (parametersStart == null) {
                        parametersStart = tokenizer.advance();
                    }
                    else {
                        parametersEnd = tokenizer.advance();
                    }
                }
            }
            return this.nodeFactory.atRule(name, parametersStart ? tokenizer.slice(parametersStart, parametersEnd) : '', rulelist);
        };
        /**
         * Consumes tokens from a Tokenizer to produce a Rulelist node.
         * @param {Tokenizer} tokenizer A Tokenizer instance.
         * @return {object} A Rulelist node.
         */
        Parser.prototype.parseRulelist = function (tokenizer) {
            var rules = [];
            // Take the opening { boundary:
            tokenizer.advance();
            while (tokenizer.currentToken) {
                if (tokenizer.currentToken.is(token_2.Token.type.closeBrace)) {
                    tokenizer.advance();
                    break;
                }
                else {
                    var rule = this.parseRule(tokenizer);
                    if (rule) {
                        rules.push(rule);
                    }
                }
            }
            return this.nodeFactory.rulelist(rules);
        };
        /**
         * Consumes tokens from a Tokenizer instance to produce a Declaration node or
         * a Ruleset node, as appropriate.
         * @param {Tokenizer} tokenizer A Tokenizer node.
         * @return {object} One of a Declaration or Ruleset node.
         */
        Parser.prototype.parseDeclarationOrRuleset = function (tokenizer) {
            var ruleStart = null;
            var ruleEnd = null;
            var colon = null;
            while (tokenizer.currentToken) {
                if (tokenizer.currentToken.is(token_2.Token.type.whitespace)) {
                    tokenizer.advance();
                }
                else if (tokenizer.currentToken.is(token_2.Token.type.openParenthesis)) {
                    while (tokenizer.currentToken &&
                        !tokenizer.currentToken.is(token_2.Token.type.closeParenthesis)) {
                        tokenizer.advance();
                    }
                }
                else if (tokenizer.currentToken.is(token_2.Token.type.openBrace) ||
                    tokenizer.currentToken.is(token_2.Token.type.propertyBoundary)) {
                    break;
                }
                else {
                    if (tokenizer.currentToken.is(token_2.Token.type.colon)) {
                        colon = tokenizer.currentToken;
                    }
                    if (!ruleStart) {
                        ruleStart = tokenizer.advance();
                        ruleEnd = ruleStart;
                    }
                    else {
                        ruleEnd = tokenizer.advance();
                    }
                }
            }
            // A ruleset never contains or ends with a semi-colon.
            if (tokenizer.currentToken.is(token_2.Token.type.propertyBoundary)) {
                var declarationName = tokenizer.slice(ruleStart, colon ? colon.previous : ruleEnd);
                // TODO(cdata): is .trim() bad for performance?
                var expressionValue = colon && tokenizer.slice(colon.next, ruleEnd).trim();
                if (tokenizer.currentToken.is(token_2.Token.type.semicolon)) {
                    tokenizer.advance();
                }
                return this.nodeFactory.declaration(declarationName, expressionValue && this.nodeFactory.expression(expressionValue));
            }
            else if (colon && colon === ruleEnd) {
                var rulelist = this.parseRulelist(tokenizer);
                if (tokenizer.currentToken.is(token_2.Token.type.semicolon)) {
                    tokenizer.advance();
                }
                return this.nodeFactory.declaration(tokenizer.slice(ruleStart, ruleEnd.previous), rulelist);
            }
            else {
                return this.nodeFactory.ruleset(tokenizer.slice(ruleStart, ruleEnd), this.parseRulelist(tokenizer));
            }
        };
        return Parser;
    }());
    exports.Parser = Parser;
});
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
define("shady-css", ["require", "exports", "shady-css/common", "shady-css/token", "shady-css/tokenizer", "shady-css/node-factory", "shady-css/node-visitor", "shady-css/stringifier", "shady-css/parser"], function (require, exports, common_4, token_3, tokenizer_2, node_factory_2, node_visitor_2, stringifier_1, parser_1) {
    "use strict";
    exports.nodeType = common_4.nodeType;
    exports.Token = token_3.Token;
    exports.Tokenizer = tokenizer_2.Tokenizer;
    exports.NodeFactory = node_factory_2.NodeFactory;
    exports.NodeVisitor = node_visitor_2.NodeVisitor;
    exports.Stringifier = stringifier_1.Stringifier;
    exports.Parser = parser_1.Parser;
});
//# sourceMappingURL=shady-css.concat.js.map