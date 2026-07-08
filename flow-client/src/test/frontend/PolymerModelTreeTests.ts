import { expect } from '@open-wc/testing';
import { createModelTree } from '../../main/frontend/internal/PolymerModelTree';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { StateNode } from '../../main/frontend/internal/StateNode';

const ELEMENT_PROPERTIES = 1;

// A minimal StateTree stub sufficient to construct StateNodes.
const treeStub: any = {
  getNode: () => null,
  getFeatureDebugName: () => '',
  isActive: () => true,
  sendNodePropertySyncToServer: () => {}
};

describe('PolymerModelTree.createModelTree', () => {
  afterEach(() => Reactive.flush());

  it('returns scalars unchanged', () => {
    expect(createModelTree('hello')).to.equal('hello');
    expect(createModelTree(42)).to.equal(42);
  });

  it('wraps a map property as a single-key object', () => {
    const node = new StateNode(3, treeStub);
    const property = node.getMap(ELEMENT_PROPERTIES).getProperty('x');
    property.setValue('v');
    expect(createModelTree(property)).to.deep.equal({ x: 'v' });
  });

  it('converts a state node with element properties and tags it with the node id', () => {
    const node = new StateNode(7, treeStub);
    node.getMap(ELEMENT_PROPERTIES).getProperty('label').setValue('Hi');
    node.getMap(ELEMENT_PROPERTIES).getProperty('count').setValue(2);
    expect(createModelTree(node)).to.deep.equal({ label: 'Hi', count: 2, nodeId: 7 });
  });
});
