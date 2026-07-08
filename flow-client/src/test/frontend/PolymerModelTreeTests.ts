import { expect } from '@open-wc/testing';
import { createModelTree } from '../../main/frontend/internal/PolymerModelTree';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { StateNode } from '../../main/frontend/internal/StateNode';

const ELEMENT_PROPERTIES = 1;
const TEMPLATE_MODELLIST = 16;

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

  // Ported from GwtPolymerModelTest.testPolymerUtilsStoreNodeIdNotAvailableAsListItem:
  // a model-list node is converted to an array tagged with its nodeId, but the
  // nodeId is stored as a non-index property so it never appears as a list item.
  it('tags a model-list node with a nodeId that is not a serialized list item', () => {
    const node = new StateNode(98, treeStub);
    node.getList(TEMPLATE_MODELLIST).add(0, 'one');
    node.getList(TEMPLATE_MODELLIST).add(1, 'two');

    const modelTree = createModelTree(node);

    // The converted value is the list array, tagged with the node id.
    expect(Array.isArray(modelTree)).to.equal(true);
    expect((modelTree as unknown as { nodeId: number }).nodeId).to.equal(98);
    // The nodeId is not a real array element, so it is not serialized as a list item.
    expect(JSON.stringify(modelTree)).to.not.contain('98');
    expect(JSON.stringify(modelTree)).to.equal('["one","two"]');
  });
});
