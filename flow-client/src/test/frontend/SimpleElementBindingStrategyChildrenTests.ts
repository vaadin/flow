import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { bindChildren, BindingContext } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';
import {
  BindGuardStateNode,
  type CollectingTree,
  NodeFeatures,
  NodeProperties,
  StateNode,
  bind,
  makeCollectingTree
} from './bindingTestHelpers';

const ELEMENT_CHILDREN = 2;

// A child state-node stand-in backed by a DOM element created lazily by the
// binder context.
function fakeChild(id: number, existingMap: { getElement(id: number): Node | null; remove(id: number): void }) {
  let domNode: Node | null = null;
  return {
    getId: () => id,
    getDomNode: () => domNode,
    setDomNode: (node: Node | null) => {
      domNode = node;
    },
    getTree: () => ({ getRegistry: () => ({ getExistingElementMap: () => existingMap }) })
  };
}

// A NodeList stand-in for ELEMENT_CHILDREN, with a hook to fire splices.
function fakeChildList(children: any[], cleared = false) {
  const listeners: Array<(event: any) => void> = [];
  return {
    children,
    length: () => children.length,
    get: (i: number) => children[i],
    hasBeenCleared: () => cleared,
    forEach: (cb: (child: unknown) => void) => children.forEach(cb),
    addSpliceListener(listener: (event: any) => void) {
      listeners.push(listener);
      return { remove: () => listeners.splice(listeners.indexOf(listener), 1) };
    },
    fireSplice(event: { isClear?: boolean; remove?: any[]; add?: any[]; index?: number }) {
      const e = {
        isClear: () => event.isClear ?? false,
        getRemove: () => event.remove ?? [],
        getAdd: () => event.add ?? [],
        getIndex: () => event.index ?? 0
      };
      listeners.forEach((l) => l(e));
    }
  };
}

function makeContext(htmlNode: Node, childList: any) {
  // The binder context creates a <span> for each child and records the binding.
  const binderContext: any = {
    createAndBind: (child: any) => {
      // Reuse an already-adopted DOM node (mirrors the real Binder), else create.
      let el = child.getDomNode();
      if (el === null) {
        el = document.createElement('span');
        el.setAttribute('data-id', String(child.getId()));
        child.setDomNode(el);
      }
      return el;
    },
    bind: () => {},
    getStrategies: () => []
  };
  const node: any = {
    getId: () => 1,
    getDomNode: () => htmlNode,
    setDomNode: () => {},
    getMap: () => ({}),
    getList: (feature: number) => (feature === ELEMENT_CHILDREN ? childList : fakeChildList([])),
    getTree: () => ({})
  };
  return new BindingContext(node, htmlNode, binderContext);
}

describe('SimpleElementBindingStrategy children binding', () => {
  afterEach(() => Reactive.flush());

  it('appends a DOM child for each child node', () => {
    const existingMap = { getElement: () => null, remove: () => {} };
    const parent = document.createElement('div');
    const list = fakeChildList([fakeChild(2, existingMap), fakeChild(3, existingMap)]);
    bindChildren(makeContext(parent, list));
    expect(parent.children).to.have.length(2);
    expect(parent.children[0].getAttribute('data-id')).to.equal('2');
    expect(parent.children[1].getAttribute('data-id')).to.equal('3');
  });

  it('adopts an existing element instead of creating a new one', () => {
    const existing = document.createElement('span');
    existing.setAttribute('data-existing', 'yes');
    const existingMap = { getElement: (id: number) => (id === 2 ? existing : null), remove: () => {} };
    const parent = document.createElement('div');
    const child = fakeChild(2, existingMap);
    const list = fakeChildList([child]);
    bindChildren(makeContext(parent, list));
    // The existing element is adopted (its DOM node set) but not appended here.
    expect(child.getDomNode()).to.equal(existing);
  });

  it('removes and adds children on splice', () => {
    const existingMap = { getElement: () => null, remove: () => {} };
    const parent = document.createElement('div');
    const first = fakeChild(2, existingMap);
    const list = fakeChildList([first]);
    const context = makeContext(parent, list);
    bindChildren(context);
    expect(parent.children).to.have.length(1);

    // Splice in a new child at the end.
    const second = fakeChild(3, existingMap);
    list.children.push(second);
    list.fireSplice({ add: [second], index: 1 });
    Reactive.flush();
    expect(parent.children).to.have.length(2);
    expect(parent.children[1].getAttribute('data-id')).to.equal('3');

    // Splice out the first child.
    list.fireSplice({ remove: [first] });
    Reactive.flush();
    expect(parent.querySelector('[data-id="2"]')).to.equal(null);
  });

  it('removes all children on a clear splice', () => {
    const existingMap = { getElement: () => null, remove: () => {} };
    const parent = document.createElement('div');
    parent.appendChild(document.createElement('b')); // client-only node
    const list = fakeChildList([fakeChild(2, existingMap)]);
    const context = makeContext(parent, list);
    bindChildren(context);
    expect(parent.childNodes.length).to.be.greaterThan(0);

    list.fireSplice({ isClear: true });
    Reactive.flush();
    expect(parent.childNodes.length).to.equal(0);
  });
});

// Full-state-tree children tests ported from GwtBasicElementBinderTest. They
// bind a real StateNode to a real <div> via the real Binder and drive child
// insertion/removal through the ELEMENT_CHILDREN node list, mirroring the
// index/position edge cases the GWT suite covers.
describe('SimpleElementBindingStrategy children binding (full tree)', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let children: ReturnType<StateNode['getList']>;
  let nextId: number;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    element = document.createElement('div');
    node.setDomNode(element);
    children = node.getList(NodeFeatures.ELEMENT_CHILDREN);
    nextId = 10;
  });

  afterEach(() => Reactive.flush());

  function createChildNode(id: string | null, tag = 'span'): StateNode {
    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);
    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
    if (id !== null) {
      childNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty('id').setValue(id);
    }
    return childNode;
  }

  // Appends a client-side-only element (no state node) to the parent, mirroring
  // GwtBasicElementBinderTest.createAndAppendElementToShadowRoot(element, ...).
  function appendClientSideChild(tag: string): Element {
    const clientChild = document.createElement(tag);
    element.appendChild(clientChild);
    return clientChild;
  }

  it('recalculates the insertion index across not-yet-bound nodes and a client-side child', () => {
    // Ported from testInsertChild_recalculateIndex.
    bind(node, element);

    appendClientSideChild('div');

    // The order is important: some state nodes don't yet have a DOM node when the
    // splices are processed, so the insertion index must be recomputed.
    children.add(0, createChildNode('first'));
    children.add(1, createChildNode('second'));
    children.add(0, createChildNode('third'));

    Reactive.flush();

    expect(element.childElementCount).to.equal(4);
    expect(element.children[0].tagName).to.equal('DIV');
    expect(element.children[1].id).to.equal('third');
    expect(element.children[2].id).to.equal('first');
    expect(element.children[3].id).to.equal('second');
  });

  it('removes a child at the right position despite an unofficial extra child', () => {
    // Ported from testRemoveChildPosition.
    bind(node, element);

    const childNode = createChildNode('child');
    children.add(0, childNode);
    Reactive.flush();

    const firstChildElement = element.firstElementChild!;

    // Add an "unofficial" (client-side) child to mess with index computations.
    const extraChild = document.createElement('img');
    element.insertBefore(extraChild, firstChildElement);

    children.splice(0, 1);
    Reactive.flush();

    expect(element.childNodes.length).to.equal(1);
    expect(element.childNodes[0]).to.equal(extraChild);
    expect(firstChildElement.parentElement).to.equal(null);
  });

  it('inserts a bound child after pre-existing client-side children', () => {
    // Ported from testInsertChildAfterExistingChildren.
    const existingChild1 = document.createElement('span');
    const existingChild2 = document.createElement('span');
    element.appendChild(existingChild1);
    element.appendChild(existingChild2);

    bind(node, element);
    Reactive.flush();
    expect(element.childElementCount).to.equal(2);

    children.add(0, createChildNode('first', 'div'));
    Reactive.flush();

    expect(element.childElementCount).to.equal(3);
    expect(element.children[0].tagName).to.equal('SPAN');
    expect(element.children[1].tagName).to.equal('SPAN');
    expect(element.children[2].tagName).to.equal('DIV');
  });

  it('re-adds a removed node reusing its existing DOM node', () => {
    // Ported from testReAddNode.
    bind(node, element);

    const childToReadd = createChildNode('2');
    children.splice(0, 0, [createChildNode('1'), childToReadd, createChildNode('3')]);

    Reactive.flush();

    const readdedDom = childToReadd.getDomNode();
    expect(element.childElementCount).to.equal(3);

    children.splice(1, 1);
    children.splice(1, 0, [childToReadd]);

    Reactive.flush();

    const statNode = children.get(1) as StateNode;
    expect(statNode).to.equal(childToReadd);
    expect(statNode.getDomNode()).to.equal(readdedDom);
  });

  // Ported from GwtMultipleBindingTest.testAddChildDoubleBind: a second bind must
  // not re-read the element-children feature.
  it('binding twice does not re-read the element-children feature', () => {
    const guarded = new BindGuardStateNode(3, harness.tree, (m) => expect.fail(m));
    harness.tree.registerNode(guarded);
    guarded.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    const guardedElement = document.createElement('div');
    guarded.setDomNode(guardedElement);

    bind(guarded, guardedElement);

    const childNode = createChildNode('child');
    guarded.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);
    Reactive.flush();

    guarded.setBound();
    bind(guarded, guardedElement);
    Reactive.flush();
  });
});
