import { expect } from '@open-wc/testing';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import type { NodeUnregisterEvent } from '../../../../../main/frontend/internal/client/flow/NodeUnregisterEvent';
import { StateTree, type Registry } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';

interface Sync {
  node: StateNode;
  mapId: number;
  name: string;
  value: unknown;
}

function makeTree(handlePropertyUpdateResult = false): {
  tree: StateTree;
  syncs: Sync[];
  getFlushCount: () => number;
  getRegisteredNodes: () => StateNode[];
} {
  const syncs: Sync[] = [];
  let flushCount = 0;
  const registeredNodes: StateNode[] = [];
  const registry: Registry = {
    getInitialPropertiesHandler: () => ({
      flushPropertyUpdates: () => {
        flushCount++;
      },
      nodeRegistered: (node) => {
        registeredNodes.push(node);
      },
      handlePropertyUpdate: () => handlePropertyUpdateResult
    }),
    getServerConnector: () => ({
      sendEventMessage: () => {},
      sendNodeSyncMessage: (node, mapId, name, value) => syncs.push({ node, mapId, name, value }),
      sendTemplateEventMessage: () => {},
      sendExistingElementAttachToServer: () => {},
      sendExistingElementWithIdAttachToServer: () => {},
      sendReturnChannelMessage: () => {}
    })
  };
  return {
    tree: new StateTree(registry),
    syncs,
    getFlushCount: () => flushCount,
    getRegisteredNodes: () => registeredNodes
  };
}

function setVisible(node: StateNode, value: boolean): void {
  node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE).setValue(value);
}

describe('StateTree', () => {
  it('maps registered nodes by id', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);

    expect(tree.getNode(node.getId())).to.equal(null);

    tree.registerNode(node);

    expect(tree.getNode(node.getId())).to.equal(node);
  });

  it('throws when registering an already-registered node', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(() => tree.registerNode(node)).to.throw();
  });

  it('throws when registering a null node', () => {
    // testRegisterNullThrows: registering null is rejected. Java asserts an
    // AssertionError via `assert node != null`; TypeScript reaches the argument
    // with a cast and registration throws before completing.
    const { tree } = makeTree();
    expect(() => tree.registerNode(null as unknown as StateNode)).to.throw();
  });

  it('fires the unregister event exactly once with the right node', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(node.isUnregistered()).to.equal(false);

    let lastEvent: NodeUnregisterEvent | null = null;
    node.addUnregisterListener((event) => {
      expect(lastEvent, 'Unexpected event fired').to.equal(null);
      lastEvent = event;
    });

    tree.unregisterNode(node);

    expect(lastEvent).to.not.equal(null);
    expect(lastEvent!.getNode()).to.equal(node);
    expect(node.isUnregistered()).to.equal(true);
    expect(tree.getNode(node.getId())).to.equal(null);
  });

  it('does not fire a removed unregister listener', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);

    const remover = node.addUnregisterListener(() => expect.fail('Should never run'));
    remover.remove();

    tree.unregisterNode(node);
  });

  it('throws when unregistering a node that was never registered', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    expect(() => tree.unregisterNode(node)).to.throw();
  });

  it('throws when unregistering a node twice', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    tree.unregisterNode(node);
    // Should run fine up to this point
    expect(() => tree.unregisterNode(node)).to.throw();
  });

  // testUpdatingTree_triggeringBinder_causesAssertionError is intentionally not
  // ported: it drives `Binder.bind`, and `Binder` is not yet ported (PORTING.md
  // rule 11). Restore this case in the PR that ports `Binder`.

  describe('sendNodePropertySyncToServer', () => {
    it('sends a non-initial property of a valid node', () => {
      const { tree, syncs } = makeTree(false);
      const property = tree.getRootNode().getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(1);
      expect(syncs[0].name).to.equal('foo');
      expect(syncs[0].value).to.equal('bar');
      expect(syncs[0].node).to.equal(tree.getRootNode());
    });

    it('does not send a property of a detached node', () => {
      const { tree, syncs } = makeTree(false);
      const node = new StateNode(7, tree);
      tree.registerNode(node);
      const property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.unregisterNode(node);
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(0);
    });

    it('does not send an initial property', () => {
      const { tree, syncs } = makeTree(true);
      const property = tree.getRootNode().getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(0);
    });
  });

  it('setUpdateInProgress flushes property updates', () => {
    // One Java @Test asserts the flush fires once after setUpdateInProgress(true)
    // and again (twice total) after setUpdateInProgress(false); kept as a single
    // it() so the case maps 1:1 to the Java method (PORTING.md rule 13.4).
    const { tree, getFlushCount } = makeTree();
    expect(getFlushCount()).to.equal(0);
    tree.setUpdateInProgress(true);
    expect(tree.isUpdateInProgress()).to.equal(true);
    expect(getFlushCount()).to.equal(1);
    tree.setUpdateInProgress(false);
    expect(getFlushCount()).to.equal(2);
  });

  it('does not call the property handler when registering while no update is in progress', () => {
    const { tree, getFlushCount, getRegisteredNodes } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(getFlushCount()).to.equal(0);
    expect(getRegisteredNodes()).to.deep.equal([]);
  });

  it('notifies the property handler when registering while an update is in progress', () => {
    const { tree, getRegisteredNodes } = makeTree();
    tree.setUpdateInProgress(true);
    const node = new StateNode(5, tree);
    tree.registerNode(node);
    expect(getRegisteredNodes()).to.deep.equal([node]);
  });

  describe('isVisible', () => {
    it('is true when the node has no element-data feature', () => {
      const { tree } = makeTree();
      expect(tree.isVisible(tree.getRootNode())).to.equal(true);
    });

    it('is true when visible is explicitly true', () => {
      const { tree } = makeTree();
      setVisible(tree.getRootNode(), true);
      expect(tree.isVisible(tree.getRootNode())).to.equal(true);
    });

    it('is true when the feature exists but has no value', () => {
      const { tree } = makeTree();
      tree.getRootNode().getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE);
      expect(tree.isVisible(tree.getRootNode())).to.equal(true);
    });

    it('is false when visible is explicitly false', () => {
      const { tree } = makeTree();
      setVisible(tree.getRootNode(), false);
      expect(tree.isVisible(tree.getRootNode())).to.equal(false);
    });
  });

  describe('isActive', () => {
    it('is true for a visible node without a parent', () => {
      const { tree } = makeTree();
      const node = new StateNode(2, tree);
      expect(tree.isActive(node)).to.equal(true);
    });

    it('is false for an invisible node without a parent', () => {
      const { tree } = makeTree();
      const node = new StateNode(2, tree);
      setVisible(node, false);
      expect(tree.isActive(node)).to.equal(false);
    });

    it('is true for a visible node with a visible parent', () => {
      const { tree } = makeTree();
      const parent = new StateNode(2, tree);
      const node = new StateNode(3, tree);
      node.setParent(parent);
      expect(tree.isActive(node)).to.equal(true);
    });

    it('is false for a visible node with an invisible parent', () => {
      const { tree } = makeTree();
      const parent = new StateNode(2, tree);
      setVisible(parent, false);
      const node = new StateNode(3, tree);
      node.setParent(parent);
      expect(tree.isActive(node)).to.equal(false);
    });
  });

  it('prepareForResync leaves only the root registered', () => {
    const { tree } = makeTree();
    const node = new StateNode(5, tree);
    tree.registerNode(node);

    tree.prepareForResync();

    expect(tree.getRootNode().isUnregistered()).to.equal(false);
    expect(node.isUnregistered()).to.equal(true);
    expect(tree.isResync()).to.equal(true);
  });
});
