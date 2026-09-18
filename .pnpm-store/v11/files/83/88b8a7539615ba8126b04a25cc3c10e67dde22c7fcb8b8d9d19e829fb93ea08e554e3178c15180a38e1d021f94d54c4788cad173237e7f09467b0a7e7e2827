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
const token_1 = require("./token");
/**
 * Class that implements tokenization of significant lexical features of the
 * CSS syntax.
 */
class Tokenizer {
    /**
     * Create a Tokenizer instance.
     * @param cssText The raw CSS string to be tokenized.
     *
     */
    constructor(cssText) {
        /**
         * Tracks the position of the tokenizer in the source string.
         * Also the default head of the Token linked list.
         */
        this.cursorToken_ = new token_1.Token(token_1.Token.type.none, 0, 0);
        /**
         * Holds a reference to a Token that is "next" in the source string, often
         * due to having been peeked at.
         */
        this.currentToken_ = null;
        this.cssText = cssText;
    }
    get offset() {
        return this.cursorToken_.end;
    }
    /**
     * The current token that will be returned by a call to `advance`. This
     * reference is useful for "peeking" at the next token ahead in the sequence.
     * If the entire CSS text has been tokenized, the `currentToken` will be null.
     */
    get currentToken() {
        if (this.currentToken_ == null) {
            this.currentToken_ = this.getNextToken_();
        }
        return this.currentToken_;
    }
    /**
     * Advance the Tokenizer to the next token in the sequence.
     * @return The current token prior to the call to `advance`, or null
     * if the entire CSS text has been tokenized.
     */
    advance() {
        let token;
        if (this.currentToken_ != null) {
            token = this.currentToken_;
            this.currentToken_ = null;
        }
        else {
            token = this.getNextToken_();
        }
        return token;
    }
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
    slice(startToken, endToken = undefined) {
        const { start, end } = this.getRange(startToken, endToken);
        return this.cssText.substring(start, end);
    }
    /**
     * Like `slice`, but returns the offsets into the source, rather than the
     * substring itself.
     */
    getRange(startToken, endToken = undefined) {
        return { start: startToken.start, end: (endToken || startToken).end };
    }
    trimRange({ start, end }) {
        while (start <= end && /\s/.test(this.cssText.charAt(start))) {
            start++;
        }
        while (start <= end && end > 0 && /\s/.test(this.cssText.charAt(end - 1))) {
            end--;
        }
        return { start, end };
    }
    /**
     * Flush all tokens from the Tokenizer.
     * @return An array of all tokens corresponding to the CSS text.
     */
    flush() {
        const tokens = [];
        while (this.currentToken) {
            tokens.push(this.advance());
        }
        return tokens;
    }
    /**
     * Extract the next token from the CSS text and advance the Tokenizer.
     * @return A Token instance, or null if the entire CSS text has beeen
     * tokenized.
     */
    getNextToken_() {
        const character = this.cssText[this.offset];
        let token;
        this.currentToken_ = null;
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
        token.previous = this.cursorToken_;
        this.cursorToken_.next = token;
        this.cursorToken_ = token;
        return token;
    }
    /**
     * Tokenize a string starting at a given offset in the CSS text. A string is
     * any span of text that is wrapped by eclusively paired, non-escaped matching
     * quotation marks.
     * @param offset An offset in the CSS text.
     * @return A string Token instance.
     */
    tokenizeString(offset) {
        const quotation = this.cssText[offset];
        let escaped = false;
        const start = offset;
        let character;
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
    }
    /**
     * Tokenize a word starting at a given offset in the CSS text. A word is any
     * span of text that is not whitespace, is not a string, is not a comment and
     * is not a structural delimiter (such as braces and semicolon).
     * @param number An offset in the CSS text.
     * @return A word Token instance.
     */
    tokenizeWord(offset) {
        const start = offset;
        let character;
        // TODO(cdata): change to greedy regex match?
        while ((character = this.cssText[offset]) &&
            !common_1.matcher.boundary.test(character)) {
            offset++;
        }
        return new token_1.Token(token_1.Token.type.word, start, offset);
    }
    /**
     * Tokenize whitespace starting at a given offset in the CSS text. Whitespace
     * is any span of text made up of consecutive spaces, tabs, newlines and other
     * single whitespace characters.
     * @param number An offset in the CSS text.
     * @return A whitespace Token instance.
     */
    tokenizeWhitespace(offset) {
        const start = offset;
        common_1.matcher.whitespaceGreedy.lastIndex = offset;
        const match = common_1.matcher.whitespaceGreedy.exec(this.cssText);
        if (match != null && match.index === offset) {
            offset = common_1.matcher.whitespaceGreedy.lastIndex;
        }
        return new token_1.Token(token_1.Token.type.whitespace, start, offset);
    }
    /**
     * Tokenize a comment starting at a given offset in the CSS text. A comment is
     * any span of text beginning with the two characters / and *, and ending with
     * a matching counterpart pair of consecurtive characters (* and /).
     * @param number An offset in the CSS text.
     * @return A comment Token instance.
     */
    tokenizeComment(offset) {
        const start = offset;
        common_1.matcher.commentGreedy.lastIndex = offset;
        const match = common_1.matcher.commentGreedy.exec(this.cssText);
        if (match == null) {
            offset = this.cssText.length;
        }
        else {
            offset = common_1.matcher.commentGreedy.lastIndex;
        }
        return new token_1.Token(token_1.Token.type.comment, start, offset);
    }
    /**
     * Tokenize a boundary at a given offset in the CSS text. A boundary is any
     * single structurally significant character. These characters include braces,
     * semicolons, the "at" symbol and others.
     * @param number An offset in the CSS text.
     * @return A boundary Token instance.
     */
    tokenizeBoundary(offset) {
        // TODO(cdata): Evaluate if this is faster than a switch statement:
        const type = token_1.boundaryTokenTypes[this.cssText[offset]] || token_1.Token.type.boundary;
        return new token_1.Token(type, offset, offset + 1);
    }
}
exports.Tokenizer = Tokenizer;
//# sourceMappingURL=tokenizer.js.map