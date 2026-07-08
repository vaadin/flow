import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { bindChildren, BindingContext } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

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
