import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { type CollectingTree, makeCollectingTree, sharedEventSettings } from '../bindingTestHelpers';
import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { bind } from '../../../../../../main/frontend/internal/client/flow/binding/Binder';

// com.vaadin.flow.shared.JsonConstants.MAP_STATE_NODE_EVENT_DATA
const MAP_STATE_NODE_EVENT_DATA = ']';

// Event data collection is exercised through a bound element: the expressions
// of a DOM listener are evaluated when the event is dispatched, and the
// resulting data is what the tree sends to the server.
//
// Beyond the Java suite: the GWT suite covers plain data and filter expressions
// (ported in SimpleElementBindingStrategyEventListenerTests) but has no case for
// the debounce filters or the state-node mapping expressions below.
describe('SimpleElementBindingStrategy event data (beyond the Java suite)', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;
  let nextId: number;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);
    element = document.createElement('div');
    node.setDomNode(element);
    document.body.appendChild(element);
    nextId = 10;
  });

  afterEach(() => {
    element.remove();
    Reactive.flush();
  });

  // Binds the node and registers a "click" listener with the given expressions.
  function bindWithClickExpressions(expressions: Record<string, unknown>): void {
    bind(node, element);
    harness.constantPool.importFromJson({ expressionsKey: sharedEventSettings(expressions) });
    node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty('click').setValue('expressionsKey');
    Reactive.flush();
  }

  type Snippet = (...captures: unknown[]) => (event: Event, element: Element) => unknown;
  type SnippetRegistry = { jsSnippets?: Record<string, Snippet> };

  // The registry that the bundle fills with the functions of declared
  // snippets, which the tests below fill by hand.
  function snippets(): Record<string, Snippet> {
    const global = window as unknown as { Vaadin?: { Flow?: SnippetRegistry } };
    global.Vaadin ??= {};
    global.Vaadin.Flow ??= {};
    global.Vaadin.Flow.jsSnippets ??= {};
    return global.Vaadin.Flow.jsSnippets;
  }

  afterEach(() => {
    delete snippets()['fn-id'];
  });

  // Binds the node and registers a "click" listener whose entries evaluate
  // declared functions, where the capture values are sent for this element
  // instead of being shared through the constant pool.
  function bindWithClickCaptures(
    shared: Record<string, { c?: number; d?: unknown; e?: string; f?: string; s?: string }>,
    captured: Record<string, unknown[]>
  ): void {
    bind(node, element);
    harness.constantPool.importFromJson({ expressionsKey: shared });
    node
      .getMap(NodeFeatures.ELEMENT_LISTENERS)
      .getProperty('click')
      .setValue(['expressionsKey', captured] as unknown as string);
    Reactive.flush();
  }

  // Adds a bound child element, so that the DOM subtree has more than one node
  // mapped to a state node.
  function addChild(tag: string): { childElement: Element; childNode: StateNode } {
    const childNode = new StateNode(nextId++, harness.tree);
    harness.tree.registerNode(childNode);
    childNode.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue(tag);
    node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);
    Reactive.flush();
    return { childElement: element.firstElementChild!, childNode };
  }

  it('sends the event immediately for a zero-timeout debounce filter', () => {
    // A debounce list of [[0]] is eager, so the filter matches at once.
    bindWithClickExpressions({ true: [[0]] });

    element.click();

    expect(harness.collectedNodes).to.have.length(1);
  });

  it('sends a leading-phase debounce immediately and swallows the events within the period', async () => {
    bindWithClickExpressions({ true: [[30, 'leading']] });

    element.click();
    expect(harness.collectedNodes).to.have.length(1);

    // Within the debounce period the event is swallowed.
    element.click();
    expect(harness.collectedNodes).to.have.length(1);

    // Once the debounce period is over, the next event is eager again.
    await new Promise((resolve) => {
      setTimeout(resolve, 50);
    });
    element.click();
    expect(harness.collectedNodes).to.have.length(2);
  });

  it('sends a trailing-phase debounce only once the period is over', async () => {
    bindWithClickExpressions({ true: [[30, 'trailing']] });

    element.click();
    // A trailing debounce is not eager, so nothing is sent yet.
    expect(harness.collectedNodes).to.have.length(0);

    await new Promise((resolve) => {
      setTimeout(resolve, 50);
    });
    expect(harness.collectedNodes).to.have.length(1);
  });

  it('maps the event target to the closest state node id', () => {
    bindWithClickExpressions({ [MAP_STATE_NODE_EVENT_DATA]: false });
    const { childElement, childNode } = addChild('span');

    // The event bubbles up from the child, whose state node is the match.
    childElement.dispatchEvent(new Event('click', { bubbles: true }));

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData[MAP_STATE_NODE_EVENT_DATA]).to.equal(childNode.getId());
  });

  it('maps an event target without a state node to its closest bound ancestor', () => {
    bindWithClickExpressions({ [MAP_STATE_NODE_EVENT_DATA]: false });
    const { childElement, childNode } = addChild('span');
    const grandchild = document.createElement('b');
    childElement.appendChild(grandchild);

    grandchild.dispatchEvent(new Event('click', { bubbles: true }));

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    // The grandchild has no state node; the closest one is the child's.
    expect(eventData[MAP_STATE_NODE_EVENT_DATA]).to.equal(childNode.getId());
  });

  it('maps an element returned by an expression to the closest state node id', () => {
    const expression = `${MAP_STATE_NODE_EVENT_DATA}element.firstElementChild`;
    bindWithClickExpressions({ [expression]: false });
    const { childNode } = addChild('span');

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData[expression]).to.equal(childNode.getId());
  });

  it('maps an element outside the state tree to -1', () => {
    const expression = `${MAP_STATE_NODE_EVENT_DATA}document.head`;
    bindWithClickExpressions({ [expression]: false });

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData[expression]).to.equal(-1);
  });

  it('evaluates a declared function with its captures, the event and the element', () => {
    snippets()['fn-id'] = (a, b) => (event, target) =>
      `${(a as number) + (b as number)} ${event.type} ${target === element}`;
    bindWithClickCaptures({ shared: { c: 2, f: 'fn-id' } }, { shared$captures: ['shared', false, 3, 4] });

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    // The captures are only reported under the key of the captured entry;
    // the shared entry itself is not evaluated without captures.
    expect(eventData).to.deep.equal({ shared$captures: '7 click true' });
  });

  it('evaluates a declared function without captures from the shared settings', () => {
    snippets()['fn-id'] = () => (event) => event.type;
    bindWithClickCaptures({ shared: { d: false, f: 'fn-id' } }, {});

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ shared: 'click' });
  });

  it('reports one value per set of captures for the same declared function', () => {
    snippets()['fn-id'] = (name) => (_event, target) => target.hasAttribute(name as string);
    bindWithClickCaptures(
      { shared: { c: 1, f: 'fn-id' } },
      { shared$first: ['shared', false, 'id'], shared$second: ['shared', false, 'title'] }
    );
    element.setAttribute('id', 'set');

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData).to.deep.equal({ shared$first: true, shared$second: false });
  });

  it('debounces a declared filter per set of captures', () => {
    snippets()['fn-id'] = (name) => (_event, target) => target.hasAttribute(name as string);
    bindWithClickCaptures(
      { shared: { c: 1, f: 'fn-id' } },
      { shared$first: ['shared', [[200, 'leading']], 'id'], shared$second: ['shared', [[200, 'leading']], 'title'] }
    );

    // Only the first filter matches, which starts its debounce period.
    element.setAttribute('id', 'set');
    element.click();
    expect(harness.collectedNodes).to.have.length(1);

    // The second filter has a debouncer of its own, so its first match is
    // eager even though the first filter is still within its period.
    element.setAttribute('title', 'set');
    element.click();
    expect(harness.collectedNodes).to.have.length(2);
  });

  it('uses the debounce settings sent with each set of captures', () => {
    snippets()['fn-id'] = (name) => (_event, target) => target.hasAttribute(name as string);
    bindWithClickCaptures(
      { shared: { c: 1, f: 'fn-id' } },
      { shared$first: ['shared', [[200, 'leading']], 'id'], shared$second: ['shared', true, 'title'] }
    );

    element.setAttribute('id', 'set');
    element.click();
    element.click();
    // The first filter is debounced, so its second match is swallowed.
    expect(harness.collectedNodes).to.have.length(1);

    element.removeAttribute('id');
    element.setAttribute('title', 'set');
    element.click();
    element.click();
    // The second filter is not debounced, even though it uses the same
    // declared function as the first one.
    expect(harness.collectedNodes).to.have.length(3);
  });

  // An entry with a side effect evaluates the filter of its listener, declared
  // with captures or as an expression without, and calls the method of the
  // event it names only when that passes.
  const sideEffectCases: Array<{
    name: string;
    shared: Record<string, { c?: number; d?: unknown; e?: string; f?: string; s: string }>;
    captured: Record<string, unknown[]>;
  }> = [
    {
      name: 'preventDefault of a declared function with captures',
      shared: { shared: { c: 1, f: 'fn-id', s: 'preventDefault' } },
      captured: { shared$match: ['shared', false, 'id'] }
    },
    {
      name: 'preventDefault of an expression',
      shared: { shared: { e: "element.hasAttribute('id')", d: false, s: 'preventDefault' } },
      captured: {}
    },
    {
      name: 'stopPropagation of an expression',
      shared: { shared: { e: "element.hasAttribute('id')", d: false, s: 'stopPropagation' } },
      captured: {}
    }
  ];

  sideEffectCases.forEach(({ name, shared, captured }) => {
    it(`calls ${name} only when the entry is truthy`, () => {
      snippets()['fn-id'] = (attribute) => (_event, target) => target.hasAttribute(attribute as string);
      const sideEffect = Object.values(shared)[0].s;
      let parentCalls = 0;
      const countParentCall = (): void => {
        parentCalls += 1;
      };
      document.body.addEventListener('click', countParentCall);
      try {
        bindWithClickCaptures(shared, captured);

        const notMatching = new MouseEvent('click', { bubbles: true, cancelable: true });
        element.dispatchEvent(notMatching);

        element.setAttribute('id', 'set');
        const matching = new MouseEvent('click', { bubbles: true, cancelable: true });
        element.dispatchEvent(matching);

        if (sideEffect === 'preventDefault') {
          expect(notMatching.defaultPrevented).to.be.false;
          expect(matching.defaultPrevented).to.be.true;
        } else {
          // Only the event that did not match reaches the parent
          expect(parentCalls).to.equal(1);
        }
      } finally {
        document.body.removeEventListener('click', countParentCall);
      }
    });
  });

  it('decodes an element capture into a DOM node', () => {
    snippets()['fn-id'] = (child) => () => (child as Element).tagName;
    // The child is added after binding, so its state node id is reserved here
    const childNodeId = nextId;
    bindWithClickCaptures(
      { shared: { c: 1, f: 'fn-id' } },
      { shared$node: ['shared', false, { '@v-node': childNodeId }] }
    );
    const { childElement, childNode } = addChild('span');
    expect(childNode.getId()).to.equal(childNodeId);

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData['shared$node']).to.equal(childElement.tagName);
  });

  it('decodes a capture of a removed node to null and still sends the event', () => {
    snippets()['fn-id'] = (child, list) => () => [child, list];
    const removedNodeId = nextId++;
    bindWithClickCaptures(
      { shared: { c: 2, f: 'fn-id' }, plain: { e: 'event.type', d: false } },
      { shared$node: ['shared', false, { '@v-node': removedNodeId }, [{ '@v-node': removedNodeId }]] }
    );

    element.click();

    const eventData = harness.collectedEventData[0] as Record<string, unknown>;
    expect(eventData).to.deep.equal({ plain: 'click', shared$node: [null, [null]] });
  });
});
