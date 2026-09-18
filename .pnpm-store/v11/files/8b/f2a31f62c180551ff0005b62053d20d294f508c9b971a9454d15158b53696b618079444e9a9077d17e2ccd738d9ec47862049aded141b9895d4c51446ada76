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
import { Range } from './common';
import { Token } from './token';
/**
 * Class that implements tokenization of significant lexical features of the
 * CSS syntax.
 */
declare class Tokenizer {
    cssText: string;
    /**
     * Tracks the position of the tokenizer in the source string.
     * Also the default head of the Token linked list.
     */
    private cursorToken_;
    /**
     * Holds a reference to a Token that is "next" in the source string, often
     * due to having been peeked at.
     */
    private currentToken_;
    /**
     * Create a Tokenizer instance.
     * @param cssText The raw CSS string to be tokenized.
     *
     */
    constructor(cssText: string);
    readonly offset: number;
    /**
     * The current token that will be returned by a call to `advance`. This
     * reference is useful for "peeking" at the next token ahead in the sequence.
     * If the entire CSS text has been tokenized, the `currentToken` will be null.
     */
    readonly currentToken: Token | null;
    /**
     * Advance the Tokenizer to the next token in the sequence.
     * @return The current token prior to the call to `advance`, or null
     * if the entire CSS text has been tokenized.
     */
    advance(): Token | null;
    /**
     * Extract a slice from the CSS text, using two tokens to represent the range
     * of text to be extracted. The extracted text will include all text between
     * the start index of the first token and the offset index of the second token
     * (or the offset index of the first token if the second is not provided).
     * @param startToken The token that represents the beginning of the
     * text range to be extracted.
     * @param endToken The token that represents the end of the text range
     * to be extracted. Defaults to the startToken if no endToken is provided.
     * @return The substring of the CSS text corresponding to the
     * startToken and endToken.
     */
    slice(startToken: Token, endToken?: Token | undefined | null): string;
    /**
     * Like `slice`, but returns the offsets into the source, rather than the
     * substring itself.
     */
    getRange(startToken: Token, endToken?: Token | undefined | null): {
        start: number;
        end: number;
    };
    trimRange({start, end}: Range): Range;
    /**
     * Flush all tokens from the Tokenizer.
     * @return An array of all tokens corresponding to the CSS text.
     */
    flush(): (Token | null)[];
    /**
     * Extract the next token from the CSS text and advance the Tokenizer.
     * @return A Token instance, or null if the entire CSS text has beeen
     * tokenized.
     */
    private getNextToken_();
    /**
     * Tokenize a string starting at a given offset in the CSS text. A string is
     * any span of text that is wrapped by eclusively paired, non-escaped matching
     * quotation marks.
     * @param offset An offset in the CSS text.
     * @return A string Token instance.
     */
    tokenizeString(offset: number): Token;
    /**
     * Tokenize a word starting at a given offset in the CSS text. A word is any
     * span of text that is not whitespace, is not a string, is not a comment and
     * is not a structural delimiter (such as braces and semicolon).
     * @param number An offset in the CSS text.
     * @return A word Token instance.
     */
    tokenizeWord(offset: number): Token;
    /**
     * Tokenize whitespace starting at a given offset in the CSS text. Whitespace
     * is any span of text made up of consecutive spaces, tabs, newlines and other
     * single whitespace characters.
     * @param number An offset in the CSS text.
     * @return A whitespace Token instance.
     */
    tokenizeWhitespace(offset: number): Token;
    /**
     * Tokenize a comment starting at a given offset in the CSS text. A comment is
     * any span of text beginning with the two characters / and *, and ending with
     * a matching counterpart pair of consecurtive characters (* and /).
     * @param number An offset in the CSS text.
     * @return A comment Token instance.
     */
    tokenizeComment(offset: number): Token;
    /**
     * Tokenize a boundary at a given offset in the CSS text. A boundary is any
     * single structurally significant character. These characters include braces,
     * semicolons, the "at" symbol and others.
     * @param number An offset in the CSS text.
     * @return A boundary Token instance.
     */
    tokenizeBoundary(offset: number): Token;
}
export { Tokenizer };
