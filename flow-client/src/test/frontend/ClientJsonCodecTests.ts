import { expect } from '@open-wc/testing';
import { applyCaptures, createReturnChannelCallback } from '../../main/frontend/internal/ClientJsonCodec';

describe('ClientJsonCodec', () => {
  it('createReturnChannelCallback forwards all arguments to the sender', () => {
    let received: unknown[] | undefined;
    const callback = createReturnChannelCallback((args) => {
      received = args;
    });
    callback('a', 1, true);
    expect(received).to.eql(['a', 1, true]);
  });

  it('applyCaptures prepends captures before the runtime arguments', () => {
    let seen: unknown[] | undefined;
    const fn = (...args: unknown[]) => {
      seen = args;
      return args.length;
    };
    const wrapped = applyCaptures(fn, [1, 2]);
    const result = wrapped(3, 4);
    expect(seen).to.eql([1, 2, 3, 4]);
    expect(result).to.equal(4);
  });

  it('applyCaptures leaves this controlled by the caller', () => {
    const host = { id: 'host' };
    let calledWithHost = false;
    const fn = function (this: unknown) {
      calledWithHost = this === host;
    };
    const wrapped = applyCaptures(fn, []);
    wrapped.call(host);
    expect(calledWithHost).to.be.true;
  });
});
