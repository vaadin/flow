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

import {expect} from 'chai';

import {Token} from '../shady-css/token';
import {Tokenizer} from '../shady-css/tokenizer';

import * as fixtures from './fixtures';
import * as helpers from './helpers';

describe('Tokenizer', () => {
  describe('when tokenizing basic structures', () => {
    it('can identify strings', () => {
      expect(new Tokenizer('"foo"').flush()).to.be.eql(helpers.linkedTokens([
        new Token(Token.type.string, 0, 5)
      ]));
    });

    it('can identify comments', () => {
      expect(new Tokenizer('/*foo*/').flush()).to.be.eql(helpers.linkedTokens([
        new Token(Token.type.comment, 0, 7)
      ]));
    });

    it('can identify words', () => {
      expect(new Tokenizer('font-family').flush())
          .to.be.eql(helpers.linkedTokens([new Token(Token.type.word, 0, 11)]));
    });

    it('can identify boundaries', () => {
      expect(new Tokenizer('@{};()').flush()).to.be.eql(helpers.linkedTokens([
        new Token(Token.type.at, 0, 1),
        new Token(Token.type.openBrace, 1, 2),
        new Token(Token.type.closeBrace, 2, 3),
        new Token(Token.type.semicolon, 3, 4),
        new Token(Token.type.openParenthesis, 4, 5),
        new Token(Token.type.closeParenthesis, 5, 6)
      ]));
    });
  });

  describe('when tokenizing standard CSS structures', () => {
    it('can tokenize a basic ruleset', () => {
      helpers.expectTokenSequence(new Tokenizer(fixtures.basicRuleset), [
        Token.type.whitespace, '\n',   Token.type.word,       'body',
        Token.type.whitespace, ' ',    Token.type.openBrace,  '{',
        Token.type.whitespace, '\n  ', Token.type.word,       'margin',
        Token.type.colon,      ':',    Token.type.whitespace, ' ',
        Token.type.word,       '0',    Token.type.semicolon,  ';',
        Token.type.whitespace, '\n  ', Token.type.word,       'padding',
        Token.type.colon,      ':',    Token.type.whitespace, ' ',
        Token.type.word,       '0px',  Token.type.whitespace, '\n',
        Token.type.closeBrace, '}',    Token.type.whitespace, '\n'
      ]);
    });

    it('can tokenize @rules', () => {
      helpers.expectTokenSequence(new Tokenizer(fixtures.atRules), [
        Token.type.whitespace,
        '\n',
        Token.type.at,
        '@',
        Token.type.word,
        'import',
        Token.type.whitespace,
        ' ',
        Token.type.word,
        'url',
        Token.type.openParenthesis,
        '(',
        Token.type.string,
        '\'foo.css\'',
        Token.type.closeParenthesis,
        ')',
        Token.type.semicolon,
        ';',
        Token.type.whitespace,
        '\n\n',
        Token.type.at,
        '@',
        Token.type.word,
        'font-face',
        Token.type.whitespace,
        ' ',
        Token.type.openBrace,
        '{',
        Token.type.whitespace,
        '\n  ',
        Token.type.word,
        'font-family',
        Token.type.colon,
        ':',
        Token.type.whitespace,
        ' ',
        Token.type.word,
        'foo',
        Token.type.semicolon,
        ';',
        Token.type.whitespace,
        '\n',
        Token.type.closeBrace,
        '}',
        Token.type.whitespace,
        '\n\n',
        Token.type.at,
        '@',
        Token.type.word,
        'charset',
        Token.type.whitespace,
        ' ',
        Token.type.string,
        '\'foo\'',
        Token.type.semicolon,
        ';',
        Token.type.whitespace,
        '\n'
      ]);
    });

    it('navigates pathological boundary usage', () => {
      helpers.expectTokenSequence(new Tokenizer(fixtures.extraSemicolons), [
        Token.type.whitespace, '\n',      Token.type.colon,      ':',
        Token.type.word,       'host',    Token.type.whitespace, ' ',
        Token.type.openBrace,  '{',       Token.type.whitespace, '\n  ',
        Token.type.word,       'margin',  Token.type.colon,      ':',
        Token.type.whitespace, ' ',       Token.type.word,       '0',
        Token.type.semicolon,  ';',       Token.type.semicolon,  ';',
        Token.type.semicolon,  ';',       Token.type.whitespace, '\n  ',
        Token.type.word,       'padding', Token.type.colon,      ':',
        Token.type.whitespace, ' ',       Token.type.word,       '0',
        Token.type.semicolon,  ';',       Token.type.semicolon,  ';',
        Token.type.whitespace, '\n  ',    Token.type.semicolon,  ';',
        Token.type.word,       'display', Token.type.colon,      ':',
        Token.type.whitespace, ' ',       Token.type.word,       'block',
        Token.type.semicolon,  ';',       Token.type.whitespace, '\n',
        Token.type.closeBrace, '}',       Token.type.semicolon,  ';',
        Token.type.whitespace, '\n'
      ]);
    });
  });

  describe('when extracting substrings', () => {
    it('can slice the string using tokens', () => {
      const tokenizer = new Tokenizer('foo bar');
      const substring = tokenizer.slice(
          new Token(Token.type.word, 2, 3), new Token(Token.type.word, 5, 6));
      expect(substring).to.be.eql('o ba');
    });
  });
});
