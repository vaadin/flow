import { expect } from '@open-wc/testing';
import {
  applyCaptures,
  createReturnChannelCallback,
  decodeStateNode
} from '../../main/frontend/internal/ClientJsonCodec';

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

  describe('decodeStateNode', () => {
    const node = { id: 5 };
    const tree = { getNode: (nodeId: number) => (nodeId === 5 ? node : null) };

    it('resolves an @v-node element reference to its state node', () => {
      expect(decodeStateNode(tree, { '@v-node': 5 })).to.equal(node);
      expect(decodeStateNode(tree, { '@v-node': 9 })).to.equal(null);
    });

    it('returns null for non-element values', () => {
      expect(decodeStateNode(tree, 'a string')).to.equal(null);
      expect(decodeStateNode(tree, 42)).to.equal(null);
      expect(decodeStateNode(tree, [1, 2])).to.equal(null);
      expect(decodeStateNode(tree, { other: 1 })).to.equal(null);
    });

    it('throws when @v-node is not a number', () => {
      expect(() => decodeStateNode(tree, { '@v-node': 'x' })).to.throw('@v-node value must be a number');
    });
  });
});
