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
/**
 * An enumeration of Token types.
 */
export declare enum TokenType {
    none = 0,
    whitespace,
    string,
    comment,
    word,
    boundary,
    propertyBoundary,
    openParenthesis,
    closeParenthesis,
    at,
    openBrace,
    closeBrace,
    semicolon,
    colon,
    hyphen,
    underscore,
}
/**
 * Class that describes individual tokens as produced by the Tokenizer.
 */
declare class Token {
    static type: typeof TokenType;
    readonly type: TokenType;
    readonly start: number;
    readonly end: number;
    previous: Token | null;
    next: Token | null;
    /**
     * Create a Token instance.
     * @param type The lexical type of the Token.
     * @param start The start index of the text corresponding to the
     * Token in the CSS text.
     * @param end The end index of the text corresponding to the Token
     * in the CSS text.
     */
    constructor(type: TokenType, start: number, end: number);
    /**
     * Test if the Token matches a given numeric type. Types match if the bitwise
     * AND of the Token's type and the argument type are equivalent to the
     * argument type.
     * @param type The numeric type to test for equivalency with the
     * Token.
     */
    is(type: TokenType): boolean;
}
/**
 * A mapping of boundary token text to their corresponding types.
 */
declare const boundaryTokenTypes: {
    [boundaryText: string]: TokenType | undefined;
};
export { Token, boundaryTokenTypes };
