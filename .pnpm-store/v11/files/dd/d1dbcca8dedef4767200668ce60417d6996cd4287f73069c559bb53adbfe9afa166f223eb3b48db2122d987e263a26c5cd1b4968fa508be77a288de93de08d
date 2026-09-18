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
describe('Token', () => {
    it('supports bitfield type comparison', () => {
        const token = new token_1.Token(128 | 32 | 2, 0, 0);
        chai_1.expect(token.is(2)).to.be.ok;
        chai_1.expect(token.is(4)).to.not.be.ok;
        chai_1.expect(token.is(32 | 2)).to.be.ok;
        chai_1.expect(token.is(4 | 64)).to.not.be.ok;
        chai_1.expect(token.is(128)).to.be.ok;
    });
});
//# sourceMappingURL=token-test.js.map