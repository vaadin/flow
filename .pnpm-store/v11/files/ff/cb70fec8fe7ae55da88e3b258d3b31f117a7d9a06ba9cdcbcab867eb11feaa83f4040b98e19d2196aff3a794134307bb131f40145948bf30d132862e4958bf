/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments)).next());
    });
};
const chai_1 = require("chai");
const cancel_token_1 = require("../cancel-token");
describe('Cancel', () => {
    it('it can be constructed', () => {
        const noArgCancel = new cancel_token_1.Cancel();
        chai_1.assert.equal(noArgCancel.toString(), 'Cancel');
        chai_1.assert.equal(noArgCancel.message, '');
        const stringCancel = new cancel_token_1.Cancel('my message');
        chai_1.assert.equal(stringCancel.toString(), 'Cancel: my message');
        chai_1.assert.equal(stringCancel.message, 'my message');
    });
    // TODO(rictic).
    it.skip('can be called without new');
});
describe('CancelToken', () => {
    it('can be constructed', () => {
        const token = new cancel_token_1.CancelToken(() => null);
        chai_1.assert.isFunction(token.throwIfRequested);
        chai_1.assert.isUndefined(token.reason);
        token.throwIfRequested(); // not cancelled, does nothing.
        chai_1.assert.equal(token[Symbol.toStringTag], 'CancelToken');
    });
    it('when cancelled, it will throw if requested', () => __awaiter(this, void 0, void 0, function* () {
        let cancelFunc = undefined;
        const token = new cancel_token_1.CancelToken(c => cancelFunc = c);
        token.throwIfRequested();
        cancelFunc();
        const error = getThrownError(() => token.throwIfRequested());
        if (!(error instanceof cancel_token_1.Cancel)) {
            throw new Error('Expected throwIfRequested to throw a Cancel');
        }
        chai_1.assert.equal(error.message, '');
        chai_1.assert.equal(token.reason, error);
        chai_1.assert.equal(yield token.promise, error);
    }));
    it('will ignore all cancels past the first', () => {
        const source = cancel_token_1.CancelToken.source();
        source.cancel('foo');
        const error1 = getThrownError(() => source.token.throwIfRequested());
        chai_1.assert.equal(error1.message, 'foo');
        source.cancel('bar');
        const error2 = getThrownError(() => source.token.throwIfRequested());
        chai_1.assert.equal(error2.message, 'foo');
        chai_1.assert.equal(error1, error2);
    });
    describe.skip('.race()', () => { });
});
function getThrownError(f) {
    try {
        f();
    }
    catch (e) {
        return e;
    }
    throw new Error('Expected f to throw but it did not.');
}
describe('isCancel', () => {
    it('returns true for our Cancel objects', () => {
        chai_1.assert.isTrue(cancel_token_1.isCancel(new cancel_token_1.Cancel()));
        chai_1.assert.isTrue(cancel_token_1.isCancel(new cancel_token_1.Cancel('message')));
    });
    it('returns false for other kinds of objects', () => {
        chai_1.assert.isFalse(cancel_token_1.isCancel(false));
        chai_1.assert.isFalse(cancel_token_1.isCancel(null));
        chai_1.assert.isFalse(cancel_token_1.isCancel(undefined));
        chai_1.assert.isFalse(cancel_token_1.isCancel({}));
        chai_1.assert.isFalse(cancel_token_1.isCancel(new cancel_token_1.CancelToken(() => null)));
    });
    it('returns true for foreign Cancel objects', () => {
        const foreignCancelClass = class Cancel {
        };
        chai_1.assert.isTrue(cancel_token_1.isCancel(new foreignCancelClass()));
        const foreignCancelFunction = (function Cancel() { });
        chai_1.assert.isTrue(cancel_token_1.isCancel(new foreignCancelFunction()));
    });
});
//# sourceMappingURL=test.js.map