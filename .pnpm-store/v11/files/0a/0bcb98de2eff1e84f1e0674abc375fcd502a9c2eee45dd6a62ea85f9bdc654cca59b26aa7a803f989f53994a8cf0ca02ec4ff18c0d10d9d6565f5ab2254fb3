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
function expectTokenType(token, type) {
    chai_1.expect(token).to.be.ok;
    chai_1.expect(token.type).to.be.equal(type);
}
exports.expectTokenType = expectTokenType;
function expectTokenSequence(lexer, sequence) {
    const lexedSequence = [];
    let token;
    while (token = lexer.advance()) {
        lexedSequence.push(token.type, lexer.slice(token));
    }
    chai_1.expect(lexedSequence).to.be.eql(sequence);
}
exports.expectTokenSequence = expectTokenSequence;
function linkedTokens(tokens) {
    tokens.reduce(function (l, r) {
        if (l) {
            l.next = r;
        }
        if (r) {
            r.previous = l;
        }
        return r;
    }, new token_1.Token(token_1.Token.type.none, 0, 0));
    return tokens;
}
exports.linkedTokens = linkedTokens;
//# sourceMappingURL=helpers.js.map