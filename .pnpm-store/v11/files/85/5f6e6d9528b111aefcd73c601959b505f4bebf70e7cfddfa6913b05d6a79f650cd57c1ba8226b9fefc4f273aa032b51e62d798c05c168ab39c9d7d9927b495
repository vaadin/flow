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
const chai_1 = require("chai");
const token_1 = require("../shady-css/token");
const tokenizer_1 = require("../shady-css/tokenizer");
const fixtures = require("./fixtures");
const helpers = require("./helpers");
describe('Tokenizer', () => {
    describe('when tokenizing basic structures', () => {
        it('can identify strings', () => {
            chai_1.expect(new tokenizer_1.Tokenizer('"foo"').flush()).to.be.eql(helpers.linkedTokens([
                new token_1.Token(token_1.Token.type.string, 0, 5)
            ]));
        });
        it('can identify comments', () => {
            chai_1.expect(new tokenizer_1.Tokenizer('/*foo*/').flush()).to.be.eql(helpers.linkedTokens([
                new token_1.Token(token_1.Token.type.comment, 0, 7)
            ]));
        });
        it('can identify words', () => {
            chai_1.expect(new tokenizer_1.Tokenizer('font-family').flush())
                .to.be.eql(helpers.linkedTokens([new token_1.Token(token_1.Token.type.word, 0, 11)]));
        });
        it('can identify boundaries', () => {
            chai_1.expect(new tokenizer_1.Tokenizer('@{};()').flush()).to.be.eql(helpers.linkedTokens([
                new token_1.Token(token_1.Token.type.at, 0, 1),
                new token_1.Token(token_1.Token.type.openBrace, 1, 2),
                new token_1.Token(token_1.Token.type.closeBrace, 2, 3),
                new token_1.Token(token_1.Token.type.semicolon, 3, 4),
                new token_1.Token(token_1.Token.type.openParenthesis, 4, 5),
                new token_1.Token(token_1.Token.type.closeParenthesis, 5, 6)
            ]));
        });
    });
    describe('when tokenizing standard CSS structures', () => {
        it('can tokenize a basic ruleset', () => {
            helpers.expectTokenSequence(new tokenizer_1.Tokenizer(fixtures.basicRuleset), [
                token_1.Token.type.whitespace, '\n', token_1.Token.type.word, 'body',
                token_1.Token.type.whitespace, ' ', token_1.Token.type.openBrace, '{',
                token_1.Token.type.whitespace, '\n  ', token_1.Token.type.word, 'margin',
                token_1.Token.type.colon, ':', token_1.Token.type.whitespace, ' ',
                token_1.Token.type.word, '0', token_1.Token.type.semicolon, ';',
                token_1.Token.type.whitespace, '\n  ', token_1.Token.type.word, 'padding',
                token_1.Token.type.colon, ':', token_1.Token.type.whitespace, ' ',
                token_1.Token.type.word, '0px', token_1.Token.type.whitespace, '\n',
                token_1.Token.type.closeBrace, '}', token_1.Token.type.whitespace, '\n'
            ]);
        });
        it('can tokenize @rules', () => {
            helpers.expectTokenSequence(new tokenizer_1.Tokenizer(fixtures.atRules), [
                token_1.Token.type.whitespace,
                '\n',
                token_1.Token.type.at,
                '@',
                token_1.Token.type.word,
                'import',
                token_1.Token.type.whitespace,
                ' ',
                token_1.Token.type.word,
                'url',
                token_1.Token.type.openParenthesis,
                '(',
                token_1.Token.type.string,
                '\'foo.css\'',
                token_1.Token.type.closeParenthesis,
                ')',
                token_1.Token.type.semicolon,
                ';',
                token_1.Token.type.whitespace,
                '\n\n',
                token_1.Token.type.at,
                '@',
                token_1.Token.type.word,
                'font-face',
                token_1.Token.type.whitespace,
                ' ',
                token_1.Token.type.openBrace,
                '{',
                token_1.Token.type.whitespace,
                '\n  ',
                token_1.Token.type.word,
                'font-family',
                token_1.Token.type.colon,
                ':',
                token_1.Token.type.whitespace,
                ' ',
                token_1.Token.type.word,
                'foo',
                token_1.Token.type.semicolon,
                ';',
                token_1.Token.type.whitespace,
                '\n',
                token_1.Token.type.closeBrace,
                '}',
                token_1.Token.type.whitespace,
                '\n\n',
                token_1.Token.type.at,
                '@',
                token_1.Token.type.word,
                'charset',
                token_1.Token.type.whitespace,
                ' ',
                token_1.Token.type.string,
                '\'foo\'',
                token_1.Token.type.semicolon,
                ';',
                token_1.Token.type.whitespace,
                '\n'
            ]);
        });
        it('navigates pathological boundary usage', () => {
            helpers.expectTokenSequence(new tokenizer_1.Tokenizer(fixtures.extraSemicolons), [
                token_1.Token.type.whitespace, '\n', token_1.Token.type.colon, ':',
                token_1.Token.type.word, 'host', token_1.Token.type.whitespace, ' ',
                token_1.Token.type.openBrace, '{', token_1.Token.type.whitespace, '\n  ',
                token_1.Token.type.word, 'margin', token_1.Token.type.colon, ':',
                token_1.Token.type.whitespace, ' ', token_1.Token.type.word, '0',
                token_1.Token.type.semicolon, ';', token_1.Token.type.semicolon, ';',
                token_1.Token.type.semicolon, ';', token_1.Token.type.whitespace, '\n  ',
                token_1.Token.type.word, 'padding', token_1.Token.type.colon, ':',
                token_1.Token.type.whitespace, ' ', token_1.Token.type.word, '0',
                token_1.Token.type.semicolon, ';', token_1.Token.type.semicolon, ';',
                token_1.Token.type.whitespace, '\n  ', token_1.Token.type.semicolon, ';',
                token_1.Token.type.word, 'display', token_1.Token.type.colon, ':',
                token_1.Token.type.whitespace, ' ', token_1.Token.type.word, 'block',
                token_1.Token.type.semicolon, ';', token_1.Token.type.whitespace, '\n',
                token_1.Token.type.closeBrace, '}', token_1.Token.type.semicolon, ';',
                token_1.Token.type.whitespace, '\n'
            ]);
        });
    });
    describe('when extracting substrings', () => {
        it('can slice the string using tokens', () => {
            const tokenizer = new tokenizer_1.Tokenizer('foo bar');
            const substring = tokenizer.slice(new token_1.Token(token_1.Token.type.word, 2, 3), new token_1.Token(token_1.Token.type.word, 5, 6));
            chai_1.expect(substring).to.be.eql('o ba');
        });
    });
});
//# sourceMappingURL=tokenizer-test.js.map