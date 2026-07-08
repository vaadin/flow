import { expect } from '@open-wc/testing';
import { StateNode, type StateTree } from '../../main/frontend/internal/StateNode';
import type { NodeFeature } from '../../main/frontend/internal/nodefeature/NodeFeature';

// Minimal StateTree stand-in; StateNode tests do not reach into the tree.
const tree: StateTree = {
  getNode: () => null,
  getFeatureDebugName: (id) => String(id),
  isActive: () => true,
  sendNodePropertySyncToServer: () => {}
};

class TestData {
  readonly marker = 'test-data';
}

describe('StateNode', () => {
  let node: StateNode;
  beforeEach(() => {
    node = new StateNode(1, tree);
  });

  function collectFeatures(): NodeFeature[] {
    const features: NodeFeature[] = [];
    node.forEachFeature((feature) => features.push(feature));
    return features;
  }

  it('has no features by default', () => {
    node.forEachFeature(() => {
      throw new Error('should have no features');
    });
  });

  it('creates and reuses a list feature', () => {
    const list = node.getList(1);
    expect(list.getId()).to.equal(1);
    expect(collectFeatures()).to.deep.equal([list]);

    const anotherList = node.getList(1);
    expect(anotherList).to.equal(list);
    expect(collectFeatures()).to.deep.equal([list]);
  });

  it('creates and reuses a map feature', () => {
    const map = node.getMap(1);
    expect(map.getId()).to.equal(1);
    expect(collectFeatures()).to.deep.equal([map]);

    const anotherMap = node.getMap(1);
    expect(anotherMap).to.equal(map);
  });

  it('stores and retrieves node data by type', () => {
    const data = new TestData();
    node.setNodeData(data);
    expect(node.getNodeData(TestData)).to.equal(data);
  });
});
