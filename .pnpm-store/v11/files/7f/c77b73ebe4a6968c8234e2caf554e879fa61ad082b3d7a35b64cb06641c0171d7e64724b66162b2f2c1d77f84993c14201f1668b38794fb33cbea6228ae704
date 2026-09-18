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

import {Tokenizer} from '../shady-css';
import {Token, TokenType} from '../shady-css/token';

function expectTokenType(token: Token, type: TokenType) {
  expect(token).to.be.ok;
  expect(token.type).to.be.equal(type);
}

function expectTokenSequence(
    lexer: Tokenizer, sequence: Array<TokenType|string>) {
  const lexedSequence = [];
  let token;

  while (token = lexer.advance()) {
    lexedSequence.push(token.type, lexer.slice(token));
  }

  expect(lexedSequence).to.be.eql(sequence);
}

function linkedTokens(tokens: Token[]) {
  tokens.reduce(function(l, r) {
    if (l) {
      l.next = r;
    }

    if (r) {
      r.previous = l;
    }

    return r;
  }, new Token(Token.type.none, 0, 0));

  return tokens;
}

export {expectTokenType, expectTokenSequence, linkedTokens};
