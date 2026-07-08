import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { StateNode } from '../../main/frontend/internal/StateNode';
import { ConstantPool } from '../../main/frontend/internal/ConstantPool';
import { SimpleElementBindingStrategy } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_DATA = 0;
const TAG = 'tag';

// A StateTree stub rich enough for create/isApplicable/bind of a simple element.
function makeTree() {
  const constantPool = new ConstantPool();
  const tree: any = {
    rootNode: null as any,
    constantPool,
    getNode: () => null,
    getRootNode: () => tree.rootNode,
    getFeatureDebugName: () => '',
    isActive: () => true,
    isVisible: () => true,
    sendNodePropertySyncToServer: () => {},
    sendEventToServer: () => {},
    sendExistingElementWithIdAttachToServer: () => {},
    getStateNodeForDomNode: () => null,
    getRegistry: () => ({
      getConstantPool: () => constantPool,
      getExistingElementMap: () => ({ getElement: () => null, remove: () => {} }),
      getInitialPropertiesHandler: () => ({ nodeRegistered: () => {}, flushPropertyUpdates: () => {} }),
      getApplicationConfiguration: () => ({ isWebComponentMode: () => false, getServiceUrl: () => '' })
    })
  };
  return tree;
}

// A BinderContext that creates a div via the strategy and binds nothing further.
function makeBinderContext(strategy: SimpleElementBindingStrategy): any {
  const ctx: any = {
    createAndBind: (node: StateNode) => {
      let dom = node.getDomNode();
      if (dom === null) {
        dom = strategy.create(node);
        node.setDomNode(dom);
      }
      strategy.bind(node, dom as Element, ctx);
      return dom;
    },
    bind: () => {},
    getStrategies: () => []
  };
  return ctx;
}

describe('SimpleElementBindingStrategy class', () => {
  afterEach(() => Reactive.flush());

  it('create builds the element for the node tag', () => {
    const tree = makeTree();
    const node = new StateNode(2, tree);
    node.getMap(ELEMENT_DATA).getProperty(TAG).setValue('span');
    const strategy = new SimpleElementBindingStrategy();
    const element = strategy.create(node);
    expect(element.tagName.toLowerCase()).to.equal('span');
  });

  it('isApplicable is true for a node with element data', () => {
    const tree = makeTree();
    const node = new StateNode(2, tree);
    node.getMap(ELEMENT_DATA).getProperty(TAG).setValue('div');
    expect(new SimpleElementBindingStrategy().isApplicable(node)).to.be.true;
  });

  it('bind wires properties, attributes and children of a visible element', () => {
    const tree = makeTree();
    const node = new StateNode(2, tree);
    node.getMap(ELEMENT_DATA).getProperty(TAG).setValue('div');
    // An element property and an attribute.
    node.getMap(1).getProperty('title').setValue('hi'); // ELEMENT_PROPERTIES
    node.getMap(3).getProperty('data-x').setValue('y'); // ELEMENT_ATTRIBUTES

    const strategy = new SimpleElementBindingStrategy();
    const element = strategy.create(node);
    node.setDomNode(element);
    strategy.bind(node, element, makeBinderContext(strategy));
    Reactive.flush();

    expect((element as any).title).to.equal('hi');
    expect(element.getAttribute('data-x')).to.equal('y');

    // Binding is idempotent (second bind is a no-op).
    strategy.bind(node, element, makeBinderContext(strategy));
    expect((element as any).title).to.equal('hi');
  });

  it('bind appends a child element', () => {
    const tree = makeTree();
    const parent = new StateNode(2, tree);
    parent.getMap(ELEMENT_DATA).getProperty(TAG).setValue('div');
    const child = new StateNode(3, tree);
    child.getMap(ELEMENT_DATA).getProperty(TAG).setValue('span');
    child.setParent(parent);
    parent.getList(2).add(0, child); // ELEMENT_CHILDREN

    const strategy = new SimpleElementBindingStrategy();
    const element = strategy.create(parent);
    parent.setDomNode(element);
    strategy.bind(parent, element, makeBinderContext(strategy));
    Reactive.flush();

    expect(element.children).to.have.length(1);
    expect(element.children[0].tagName.toLowerCase()).to.equal('span');
  });
});
