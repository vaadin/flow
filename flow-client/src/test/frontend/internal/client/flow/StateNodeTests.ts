import { expect } from '@open-wc/testing';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree, type Registry } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { inertRegistry } from './stateTreeTestRegistry';
import type { NodeFeature } from '../../../../../main/frontend/internal/client/flow/nodefeature/NodeFeature';

// Minimal registry; StateNode tests do not reach into the tree or the server.
const registry: Registry = inertRegistry();
const tree = new StateTree(registry);

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

  // Also the counterpart of GwtStateNodeTest.testNodeData_getNodeData_sameInstance:
  // set node data, then get the same instance back by type.
  it('stores and retrieves node data by type', () => {
    const data = new TestData();
    node.setNodeData(data);
    expect(node.getNodeData(TestData)).to.equal(data);
  });
});
