import { expect } from '@open-wc/testing';
import {
  decodeStateNode,
  decodeWithTypeInfo,
  decodeWithoutTypeInfo
} from '../../../../../../main/frontend/internal/client/flow/util/ClientJsonCodec';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree, type Registry } from '../../../../../../main/frontend/internal/client/flow/StateTree';

interface ReturnMessage {
  nodeId: number;
  channelId: number;
  args: unknown[];
}

// Builds a real StateTree from a minimal registry so the codec is exercised
// through the ported StateTree/StateNode rather than a hand-rolled stand-in.
function makeTree(sent: ReturnMessage[] = []): StateTree {
  const registry: Registry = {
    getInitialPropertiesHandler: () => ({
      flushPropertyUpdates: () => {},
      nodeRegistered: () => {},
      handlePropertyUpdate: () => false
    }),
    getServerConnector: () => ({
      sendEventMessage: () => {},
      sendNodeSyncMessage: () => {},
      sendTemplateEventMessage: () => {},
      sendExistingElementAttachToServer: () => {},
      sendExistingElementWithIdAttachToServer: () => {},
      sendReturnChannelMessage: (nodeId, channelId, args) => sent.push({ nodeId, channelId, args })
    })
  };
  return new StateTree(registry);
}

// Counterpart set: there is no JRE `ClientJsonCodecTest` or
// `JreClientJsonCodecTest` under flow-client/src/test/java, and ClientJsonCodec
// lives in com.vaadin.client (not com.vaadin.flow), so there is no flow-server
// test either. The only counterpart is `GwtClientJsonCodecTest` under
// src/test-gwt, whose two GWTTestCase-runnable `test*` methods are ported in the
// `GwtClientJsonCodecTest` block below; the remaining cases are extra coverage
// beyond the Java suite.
describe('ClientJsonCodec', () => {
  // Cases from the GWT-side counterpart GwtClientJsonCodecTest.
  // decodeWithoutTypeInfo mirrors ClientJsonCodec's GWT.isScript() branch — a
  // no-op returning the JSON as-is. The Java cases carry @Test(expected=
  // IllegalArgumentException), but GWTTestCase (JUnit 3) ignores that annotation
  // and runs in compiled-JS mode, so an array/object is returned unchanged rather
  // than rejected; the JRE-only rejection is test scaffolding with no port.
  describe('decodeWithoutTypeInfo', () => {
    it('returns an array unchanged', () => {
      // testDecodeWithoutTypeInfo_arrayUnsupported
      const array = ['string', true];
      expect(decodeWithoutTypeInfo(array)).to.equal(array);
    });

    it('returns an object unchanged', () => {
      // testDecodeWithoutTypeInfo_objectUnsupported
      const object = { key: 'value' };
      expect(decodeWithoutTypeInfo(object)).to.equal(object);
    });
  });

  describe('decodeStateNode', () => {
    let tree: StateTree;
    let node: StateNode;
    beforeEach(() => {
      tree = makeTree();
      node = new StateNode(5, tree);
      tree.registerNode(node);
    });

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
    const sent: ReturnMessage[] = [];
    let tree: StateTree;
    let domNode: Node;
    beforeEach(() => {
      sent.length = 0;
      tree = makeTree(sent);
      domNode = document.createElement('div');
      const node = new StateNode(5, tree);
      node.setDomNode(domNode);
      tree.registerNode(node);
    });

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

    // Beyond the Java suite: pins the mutate-in-place identity of the decode.
    // `ClientJsonCodec.java:306` writes the decoded values back into the incoming
    // JsonObject and returns that same instance, so the port must mutate in place
    // rather than build a copy — which `to.deep.equal` cannot tell apart.
    it('decodes an object in place, returning the same instance', () => {
      const json = { a: 1, b: { '@v-node': 5 } };

      expect(decodeWithTypeInfo(tree, json)).to.equal(json);
      expect(json.b).to.equal(domNode);
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
