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
/**
 * An enumeration of Token types.
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
})(TokenType = exports.TokenType || (exports.TokenType = {}));
/**
 * Class that describes individual tokens as produced by the Tokenizer.
 */
class Token {
    /**
     * Create a Token instance.
     * @param type The lexical type of the Token.
     * @param start The start index of the text corresponding to the
     * Token in the CSS text.
     * @param end The end index of the text corresponding to the Token
     * in the CSS text.
     */
    constructor(type, start, end) {
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
     * @param type The numeric type to test for equivalency with the
     * Token.
     */
    is(type) {
        return (this.type & type) === type;
    }
}
Token.type = TokenType;
exports.Token = Token;
/**
 * A mapping of boundary token text to their corresponding types.
 */
const boundaryTokenTypes = {
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
//# sourceMappingURL=token.js.map