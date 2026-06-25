import { expect } from '@open-wc/testing';
import {
  getClosestStateNodeIdToDomNode,
  getClosestStateNodeIdToEventTarget,
  getOrCreateExpression,
  getStateNodeForElement,
  resolveDebounces,
  resolveFilters
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

// com.vaadin.flow.internal.nodefeature.NodeFeatures.ELEMENT_CHILDREN
const ELEMENT_CHILDREN = 2;

// A StateNode stand-in for the closest-node lookups: an id, a DOM node, and an
// ELEMENT_CHILDREN list of child fakes.
function fakeNode(id: number, domNode: Node | null, children: any[] = []): any {
  return {
    getId: () => id,
    getDomNode: () => domNode,
    getList: (featureId: number) => ({
      forEach: (cb: (child: unknown) => void) => (featureId === ELEMENT_CHILDREN ? children : []).forEach(cb)
    })
  };
}

describe('SimpleElementBindingStrategy event-data helpers', () => {
  it('getOrCreateExpression compiles and caches an (event, element) function', () => {
    const expr = getOrCreateExpression('event.detail + element.tabIndex');
    expect(expr({ detail: 5 } as any, { tabIndex: 2 } as any)).to.equal(7);
    // Same string => same cached function instance.
    expect(getOrCreateExpression('event.detail + element.tabIndex')).to.equal(expr);
  });

  it('resolveDebounces treats a zero timeout as eager', () => {
    const element = document.createElement('div');
    const eager = resolveDebounces(element, 'on-click:x', [[0]], () => {}, new Map());
    expect(eager).to.be.true;
  });

  it('resolveDebounces fires a leading debounce immediately but buffers a trailing one', () => {
    const element = document.createElement('div');
    // A leading phase with a fresh debouncer triggers now.
    const eager = resolveDebounces(element, 'on-input:y', [[50, 'leading']], () => {}, new Map());
    expect(eager).to.be.true;
    // A trailing debounce buffers instead, so it is not eager.
    const element2 = document.createElement('div');
    const trailing = resolveDebounces(element2, 'on-input:y', [[50, 'trailing']], () => {}, new Map());
    expect(trailing).to.be.false;
  });

  it('resolveFilters returns true when there are no active filters', () => {
    const element = document.createElement('div');
    // All settings falsy => treated as no filters => send.
    const result = resolveFilters(element, 'click', { a: false }, null, () => {}, new Map());
    expect(result).to.be.true;
  });

  it('resolveFilters matches a boolean filter only when the event data is truthy', () => {
    const element = document.createElement('div');
    const matched = resolveFilters(element, 'click', { needsCtrl: true }, { needsCtrl: true }, () => {}, new Map());
    expect(matched).to.be.true;

    const notMatched = resolveFilters(element, 'click', { needsCtrl: true }, { needsCtrl: false }, () => {}, new Map());
    expect(notMatched).to.be.false;
  });

  it('resolveFilters resolves a debounce filter via resolveDebounces', () => {
    const element = document.createElement('div');
    // The filter is present in eventData and its settings are a debounce list
    // with an eager (zero-timeout) entry => sent now.
    const result = resolveFilters(element, 'input', { typed: [[0]] }, { typed: true }, () => {}, new Map());
    expect(result).to.be.true;
  });
});

describe('SimpleElementBindingStrategy closest-state-node lookups', () => {
  it('getClosestStateNodeIdToEventTarget returns the id on a direct DOM match', () => {
    const dom = document.createElement('div');
    const child = document.createElement('span');
    dom.appendChild(child);
    const childNode = fakeNode(2, child);
    const topNode = fakeNode(1, dom, [childNode]);
    // The target is the child element itself => direct match in the BFS.
    expect(getClosestStateNodeIdToEventTarget(topNode, child)).to.equal(2);
  });

  it('getClosestStateNodeIdToEventTarget walks up the DOM when there is no direct match', () => {
    const dom = document.createElement('div');
    const child = document.createElement('span');
    const grandchild = document.createElement('b');
    dom.appendChild(child);
    child.appendChild(grandchild);
    const childNode = fakeNode(2, child);
    const topNode = fakeNode(1, dom, [childNode]);
    // grandchild has no state node; the closest ancestor with one is the child.
    expect(getClosestStateNodeIdToEventTarget(topNode, grandchild)).to.equal(2);
  });

  it('getClosestStateNodeIdToEventTarget returns -1 for a null target', () => {
    expect(getClosestStateNodeIdToEventTarget(fakeNode(1, document.createElement('div')), null)).to.equal(-1);
  });

  it('getStateNodeForElement finds the nearest ancestor in the search stack', () => {
    const dom = document.createElement('div');
    const child = document.createElement('span');
    dom.appendChild(child);
    const stack = [fakeNode(1, dom), fakeNode(2, child)];
    expect(getStateNodeForElement(stack, child)).to.equal(2);
    // An unrelated detached node has no ancestor in the stack.
    expect(getStateNodeForElement(stack, document.createElement('p'))).to.equal(-1);
  });

  it('getClosestStateNodeIdToDomNode walks up via the tree mapping', () => {
    const dom = document.createElement('div');
    const child = document.createElement('span');
    dom.appendChild(child);
    const tree = {
      getStateNodeForDomNode: (node: Node) => (node.isSameNode(dom) ? { getId: () => 5 } : null)
    };
    // child has no mapping; its parent div does.
    expect(getClosestStateNodeIdToDomNode(tree, child, 'event.target')).to.equal(5);
    expect(getClosestStateNodeIdToDomNode(tree, null, 'event.target')).to.equal(-1);
  });
});
