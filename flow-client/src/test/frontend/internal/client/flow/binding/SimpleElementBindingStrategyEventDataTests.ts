import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { type CollectingTree, makeCollectingTree } from '../bindingTestHelpers';
import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { bind } from '../../../../../../main/frontend/internal/client/flow/binding/Binder';
import { JsonConstants } from '../../../../../../main/frontend/internal/flow/shared/JsonConstants';

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
    harness.constantPool.importFromJson({ expressionsKey: expressions });
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

  // Binds the node and registers a "click" listener. The shared settings are
  // sent through the constant pool. The settings of this element only, for
  // example a snippet that captures an element, are sent with the key.
  function bindWithClickSettings(shared: Record<string, unknown>, own: Record<string, unknown> = {}): void {
    bind(node, element);
    harness.constantPool.importFromJson({ expressionsKey: shared });
    const value = Object.keys(own).length === 0 ? 'expressionsKey' : ['expressionsKey', own];
    node
      .getMap(NodeFeatures.ELEMENT_LISTENERS)
      .getProperty('click')
      .setValue(value as unknown as string);
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

  describe('without unsafe-eval', () => {
    const originalFunction = globalThis.Function;

    // As under a Content Security Policy without unsafe-eval, where the
    // browser refuses to compile a string
    beforeEach(() => {
      globalThis.Function = function blocked(): never {
        throw new EvalError('blocked by the test');
      } as unknown as FunctionConstructor;
    });

    afterEach(() => {
      globalThis.Function = originalFunction;
    });

    it('runs the always-true filter that carries a debounce', () => {
      bindWithClickExpressions({ [JsonConstants.EVENT_FILTER_ALWAYS_TRUE]: [[200, 'leading']] });

      element.click();

      expect(harness.collectedEventData[0]).to.deep.equal({ [JsonConstants.EVENT_FILTER_ALWAYS_TRUE]: 1 });
    });

    it('reads property paths without compiling them', () => {
      bindWithClickExpressions({
        'event.type': false,
        'element.tagName.length': false,
        [`${MAP_STATE_NODE_EVENT_DATA}event.target`]: false
      });

      element.click();

      expect(harness.collectedEventData[0]).to.deep.equal({
        'event.type': 'click',
        'element.tagName.length': 3,
        [`${MAP_STATE_NODE_EVENT_DATA}event.target`]: node.getId()
      });
    });

    it('calls preventDefault without compiling it', () => {
      bindWithClickExpressions({ [JsonConstants.EVENT_DATA_PREVENT_DEFAULT]: false });

      const event = new MouseEvent('click', { bubbles: true, cancelable: true });
      element.dispatchEvent(event);

      expect(event.defaultPrevented).to.be.true;
      expect(harness.collectedNodes).to.have.length(1);
    });

    it('calls stopPropagation without compiling it', () => {
      let parentCalls = 0;
      const countParentCall = (): void => {
        parentCalls += 1;
      };
      document.body.addEventListener('click', countParentCall);
      try {
        bindWithClickExpressions({ [JsonConstants.EVENT_DATA_STOP_PROPAGATION]: false });

        element.click();

        expect(parentCalls).to.equal(0);
        expect(harness.collectedNodes).to.have.length(1);
      } finally {
        document.body.removeEventListener('click', countParentCall);
      }
    });

    it('runs the function from the bundle for an expression key with snippet settings', () => {
      // A bundled @EventData value keeps the expression as its key, so it is
      // the shape of the settings that says not to compile the key
      const expression = 'event.clientX - element.offsetLeft';
      snippets()['fn-id'] = () => (event, target) => `${event.type} ${target === element}`;
      bindWithClickSettings({ [expression]: { f: 'fn-id', d: false } });

      element.click();

      expect(harness.collectedEventData[0]).to.deep.equal({ [expression]: 'click true' });
    });

    it('maps what the function from the bundle returns for a mapped key', () => {
      const key = `${MAP_STATE_NODE_EVENT_DATA}element.firstElementChild`;
      snippets()['fn-id'] = () => (_event, target) => target.firstElementChild;
      bindWithClickSettings({ [key]: { f: 'fn-id', d: false, e: true } });
      const { childNode } = addChild('span');

      element.click();

      expect(harness.collectedEventData[0]).to.deep.equal({ [key]: childNode.getId() });
    });

    it('does not send the event for a bundled expression missing from the bundle', () => {
      // An expression of its own, as a compiled expression is cached
      const expression = 'event.type + " blocked"';
      const errors: string[] = [];
      const reported: unknown[] = [];
      const originalError = console.error;
      const onerror = window.onerror;
      console.error = (message: unknown) => errors.push(String(message));
      // The browser reports the error of a listener as uncaught; keep it from
      // failing the case
      window.onerror = (...args) => {
        reported.push(args[4]);
        return true;
      };
      try {
        bindWithClickSettings({ 'event.type': false, [expression]: { f: 'missing-bundled-blocked', d: false } });

        element.click();
      } finally {
        console.error = originalError;
        window.onerror = onerror;
      }

      expect(harness.collectedNodes).to.have.length(0);
      expect(reported).to.have.length(1);
      expect(reported[0]).to.be.instanceOf(EvalError);
      expect(errors[0]).to.contain(`@EventData value or @DomEvent filter "${expression}"`);
      expect(errors[0]).to.contain('the event is not sent');
    });
  });

  it('compiles a mapped bundled expression missing from the bundle without its prefix and maps it', () => {
    // An expression of its own, as a compiled expression is cached
    const expression = 'element.firstElementChild || null';
    const key = `${MAP_STATE_NODE_EVENT_DATA}${expression}`;
    const warnings: string[] = [];
    const originalWarn = console.warn;
    console.warn = (message: unknown) => warnings.push(String(message));
    try {
      bindWithClickSettings({ [key]: { f: 'missing-bundled-mapped', d: false, e: true } });
      const { childNode } = addChild('span');

      element.click();

      expect(harness.collectedEventData[0]).to.deep.equal({ [key]: childNode.getId() });
    } finally {
      console.warn = originalWarn;
    }
    expect(warnings).to.have.length(1);
    expect(warnings[0]).to.contain(`@EventData value or @DomEvent filter "${expression}"`);
  });

  it('compiles a bundled expression missing from the bundle and warns once', () => {
    // An expression of its own, as a compiled expression is cached
    const expression = 'event.type + " compiled"';
    const warnings: string[] = [];
    const originalWarn = console.warn;
    console.warn = (message: unknown) => warnings.push(String(message));
    try {
      bindWithClickSettings({ [expression]: { f: 'missing-bundled-compiled', d: false } });

      element.click();
      element.click();
    } finally {
      console.warn = originalWarn;
    }

    expect(harness.collectedEventData).to.deep.equal([
      { [expression]: 'click compiled' },
      { [expression]: 'click compiled' }
    ]);
    expect(warnings).to.have.length(1);
    expect(warnings[0]).to.contain(`@EventData value or @DomEvent filter "${expression}"`);
  });

  it('runs a declared snippet with its captures, the event and the element', () => {
    snippets()['fn-id'] = (a, b) => (event, target) =>
      `${(a as number) + (b as number)} ${event.type} ${target === element}`;
    bindWithClickSettings({ '@sum': { f: 'fn-id', d: false, c: [3, 4] } });

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ '@sum': '7 click true' });
  });

  it('maps a DOM node returned by a declared snippet of an element to the closest state node id', () => {
    snippets()['fn-id'] = () => (_event, target) => target.firstElementChild!.firstChild;
    bindWithClickSettings({ '@target': { f: 'fn-id', d: false, e: true } });
    const { childElement, childNode } = addChild('span');
    childElement.appendChild(document.createTextNode('text'));

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ '@target': childNode.getId() });
  });

  it('runs a declared snippet without captures', () => {
    snippets()['fn-id'] = () => (event) => event.type;
    bindWithClickSettings({ '@type': { f: 'fn-id', d: false } });

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ '@type': 'click' });
  });

  it('sends a value for each set of captures of the same function', () => {
    snippets()['fn-id'] = (name) => (_event, target) => target.hasAttribute(name as string);
    bindWithClickSettings({
      '@first': { f: 'fn-id', d: false, c: ['id'] },
      '@second': { f: 'fn-id', d: false, c: ['title'] }
    });
    element.setAttribute('id', 'set');

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ '@first': true, '@second': false });
  });

  it('sends null for a declared snippet missing from the bundle and still sends the event', () => {
    bindWithClickSettings({ 'event.type': false, '@missing': { f: 'missing-fn-id', d: false } });

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ 'event.type': 'click', '@missing': null });
  });

  it('runs expressions and declared snippets of the same listener', () => {
    snippets()['fn-id'] = () => (event) => event.type;
    bindWithClickSettings({ 'event.type': false, '@type': { f: 'fn-id', d: false } });

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ 'event.type': 'click', '@type': 'click' });
  });

  it('debounces each declared filter on its own', () => {
    snippets()['fn-id'] = (name) => (_event, target) => target.hasAttribute(name as string);
    bindWithClickSettings({
      '@first': { f: 'fn-id', d: [[200, 'leading']], c: ['id'] },
      '@second': { f: 'fn-id', d: [[200, 'leading']], c: ['title'] }
    });

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

  it('uses the debounce settings of each declared filter', () => {
    snippets()['fn-id'] = (name) => (_event, target) => target.hasAttribute(name as string);
    bindWithClickSettings({
      '@first': { f: 'fn-id', d: [[200, 'leading']], c: ['id'] },
      '@second': { f: 'fn-id', d: true, c: ['title'] }
    });

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

  ['preventDefault', 'stopPropagation'].forEach((sideEffect) => {
    it(`calls ${sideEffect} only when the declared filter passes`, () => {
      snippets()['fn-id'] = (attribute) => (_event, target) => target.hasAttribute(attribute as string);
      let parentCalls = 0;
      const countParentCall = (): void => {
        parentCalls += 1;
      };
      document.body.addEventListener('click', countParentCall);
      try {
        bindWithClickSettings({ [`@filter.${sideEffect}`]: { f: 'fn-id', d: false, s: sideEffect, c: ['id'] } });

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

  it('decodes a captured element sent for this element only', () => {
    snippets()['fn-id'] = (child) => () => (child as Element).tagName;
    // The child is added after binding, so its state node id is reserved here
    const childNodeId = nextId;
    bindWithClickSettings({}, { '@node': { f: 'fn-id', d: false, c: [{ '@v-node': childNodeId }] } });
    const { childElement, childNode } = addChild('span');
    expect(childNode.getId()).to.equal(childNodeId);

    // The stored capture is not changed by decoding, so the second event
    // decodes it again
    element.click();
    element.click();

    expect(harness.collectedEventData).to.have.length(2);
    harness.collectedEventData.forEach((eventData) =>
      expect((eventData as Record<string, unknown>)['@node']).to.equal(childElement.tagName)
    );
  });

  it('decodes a capture of a removed node to null and still sends the event', () => {
    snippets()['fn-id'] = (child) => () => child;
    const removedNodeId = nextId++;
    bindWithClickSettings(
      { 'event.type': false },
      { '@node': { f: 'fn-id', d: false, c: [{ '@v-node': removedNodeId }] } }
    );

    element.click();

    expect(harness.collectedEventData[0]).to.deep.equal({ 'event.type': 'click', '@node': null });
  });
});
