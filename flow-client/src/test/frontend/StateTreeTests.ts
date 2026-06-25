import { expect } from '@open-wc/testing';
import { StateNode } from '../../main/frontend/internal/StateNode';
import { StateTree, type Registry } from '../../main/frontend/internal/StateTree';
import { NodeFeatures, NodeProperties } from '../../main/frontend/internal/nodefeature/NodeFeatures';

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
} {
  const syncs: Sync[] = [];
  let flushCount = 0;
  const registry: Registry = {
    getInitialPropertiesHandler: () => ({
      flushPropertyUpdates: () => {
        flushCount++;
      },
      nodeRegistered: () => {},
      handlePropertyUpdate: () => handlePropertyUpdateResult
    }),
    getServerConnector: () => ({
      sendEventMessage: () => {},
      sendNodeSyncMessage: (node, mapId, name, value) => syncs.push({ node, mapId, name, value }),
      sendTemplateEventMessage: () => {},
      sendExistingElementAttachToServer: () => {},
      sendExistingElementWithIdAttachToServer: () => {}
    })
  };
  return { tree: new StateTree(registry), syncs, getFlushCount: () => flushCount };
}

function setVisible(node: StateNode, value: boolean): void {
  node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBLE).setValue(value);
}

describe('StateTree', () => {
  it('maps registered nodes by id, with the root pre-registered', () => {
    const { tree } = makeTree();
    expect(tree.getNode(1)).to.equal(tree.getRootNode());

    const node = new StateNode(2, tree);
    tree.registerNode(node);
    expect(tree.getNode(2)).to.equal(node);

    tree.unregisterNode(node);
    expect(tree.getNode(2)).to.equal(null);
    expect(node.isUnregistered()).to.equal(true);
  });

  it('setUpdateInProgress flushes property updates', () => {
    const { tree, getFlushCount } = makeTree();
    expect(getFlushCount()).to.equal(0);
    tree.setUpdateInProgress(true);
    expect(tree.isUpdateInProgress()).to.equal(true);
    expect(getFlushCount()).to.equal(1);
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

    it('does not send an initial property', () => {
      const { tree, syncs } = makeTree(true);
      const property = tree.getRootNode().getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(0);
    });

    it('does not send a property of a detached node', () => {
      const { tree, syncs } = makeTree(false);
      const detached = new StateNode(7, tree);
      const property = detached.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty('foo');
      property.setValue('bar');
      tree.sendNodePropertySyncToServer(property);
      expect(syncs.length).to.equal(0);
    });
  });

  it('prepareForResync leaves only the root registered', () => {
    const { tree } = makeTree();
    const a = new StateNode(2, tree);
    const b = new StateNode(3, tree);
    tree.registerNode(a);
    tree.registerNode(b);

    tree.prepareForResync();

    expect(tree.getNode(1)).to.equal(tree.getRootNode());
    expect(tree.getNode(2)).to.equal(null);
    expect(tree.getNode(3)).to.equal(null);
    expect(tree.isResync()).to.equal(true);
  });
});
