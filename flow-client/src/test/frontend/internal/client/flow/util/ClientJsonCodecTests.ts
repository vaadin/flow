import { expect } from '@open-wc/testing';
import {
  decodeStateNode,
  decodeWithTypeInfo
} from '../../../../../../main/frontend/internal/client/flow/util/ClientJsonCodec';

describe('ClientJsonCodec', () => {
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

  describe('decodeWithTypeInfo', () => {
    const domNode = { tag: 'div' };
    const node = { getDomNode: () => domNode };
    const sent: Array<{ nodeId: number; channelId: number; args: unknown[] }> = [];
    const tree = {
      getNode: (nodeId: number) => (nodeId === 5 ? node : null),
      getRegistry: () => ({
        getServerConnector: () => ({
          sendReturnChannelMessage: (nodeId: number, channelId: number, args: unknown[]) =>
            sent.push({ nodeId, channelId, args })
        })
      })
    };

    it('passes primitives through unchanged', () => {
      expect(decodeWithTypeInfo(tree, 'hi')).to.equal('hi');
      expect(decodeWithTypeInfo(tree, 42)).to.equal(42);
    });

    it('resolves an @v-node element reference to its DOM node', () => {
      expect(decodeWithTypeInfo(tree, { '@v-node': 5 })).to.equal(domNode);
    });

    it('decodes nested objects and arrays recursively', () => {
      expect(decodeWithTypeInfo(tree, { a: 1, b: { '@v-node': 5 } })).to.deep.equal({ a: 1, b: domNode });
      expect(decodeWithTypeInfo(tree, [1, { '@v-node': 5 }])).to.deep.equal([1, domNode]);
    });

    it('builds a return-channel callback that messages the server', () => {
      const callback = decodeWithTypeInfo(tree, { '@v-return': [5, 2] }) as (...args: unknown[]) => void;
      callback('x', 7);
      expect(sent).to.deep.equal([{ nodeId: 5, channelId: 2, args: ['x', 7] }]);
    });

    it('manifests an @v-fn function binding its captures before runtime args', () => {
      const fn = decodeWithTypeInfo(tree, {
        '@v-fn': { body: 'return $0 + a;', captures: [10], args: ['a'] }
      }) as (...args: unknown[]) => unknown;
      expect(fn(5)).to.equal(15);
    });

    it('throws on an unknown @v- type', () => {
      expect(() => decodeWithTypeInfo(tree, { '@v-bogus': 1 })).to.throw("Unsupported @v type '@v-bogus'");
    });
  });
});
